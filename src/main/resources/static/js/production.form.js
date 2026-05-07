(function() {
    'use strict';

    const form = document.getElementById('workOrderCreateForm');
    if (!form) {
        return;
    }

    const data = window.productionFormData || {};
    const products = Array.isArray(data.products) ? data.products : [];
    const productionLines = Array.isArray(data.productionLines) ? data.productionLines : [];
    const materialTemplates = data.materialTemplates || {};

    const productMap = new Map(products.map(product => [product.code, product]));
    const lineMap = new Map(productionLines.map(line => [line.code, line]));

    const statusMeta = {
        AVAILABLE: { label: 'Uygun', cssClass: 'success' },
        BUSY: { label: 'Meşgul', cssClass: 'warning' },
        MAINTENANCE: { label: 'Bakımda', cssClass: 'danger' },
        OFFLINE: { label: 'Devre Dışı', cssClass: 'neutral' }
    };

    const materialStatusMeta = {
        AVAILABLE: { label: 'Yeterli', cssClass: 'success' },
        LOW_STOCK: { label: 'Düşük Stok', cssClass: 'warning' },
        MISSING: { label: 'Eksik', cssClass: 'danger' },
        CRITICAL: { label: 'Kritik', cssClass: 'danger' }
    };

    const productSelect = document.getElementById('productCode');
    const quantityInput = document.getElementById('plannedQuantity');
    const unitInput = document.getElementById('unit');
    const lineSelect = document.getElementById('productionLineCode');
    const startDateInput = document.getElementById('plannedStartDate');
    const endDateInput = document.getElementById('plannedEndDate');

    const productInfoEmpty = document.getElementById('productInfoEmpty');
    const productInfoCard = document.getElementById('productInfoCard');
    const lineInfoEmpty = document.getElementById('lineInfoEmpty');
    const lineInfoCard = document.getElementById('lineInfoCard');

    const materialBody = document.getElementById('materialRequirementsBody');
    const materialEmpty = document.getElementById('materialRequirementsEmpty');
    const materialAlert = document.getElementById('materialRequirementsAlert');
    const mrpAction = document.getElementById('sendToMrpAction');
    const mrpActionInline = document.getElementById('mrpActionInline');
    const procurementPlaceholderAction = document.getElementById('procurementPlaceholderAction');

    function formatQuantity(value) {
        return new Intl.NumberFormat('tr-TR', {
            minimumFractionDigits: 0,
            maximumFractionDigits: 2
        }).format(Number(value || 0));
    }

    function badge(label, cssClass) {
        return '<span class="erp-badge erp-badge--' + cssClass + '">' + label + '</span>';
    }

    function getSelectedProduct() {
        return productMap.get(productSelect.value) || null;
    }

    function getSelectedLine() {
        return lineMap.get(lineSelect.value) || null;
    }

    function parseQuantity() {
        const value = Number(quantityInput.value || 0);
        return Number.isFinite(value) && value > 0 ? value : 0;
    }

    function calculateMaterialStatus(requiredQuantity, availableStock) {
        const shortage = Math.max(requiredQuantity - availableStock, 0);
        if (shortage > 0) {
            if (availableStock === 0 || shortage >= requiredQuantity * 0.5) {
                return 'CRITICAL';
            }
            return 'MISSING';
        }
        const remaining = availableStock - requiredQuantity;
        if (remaining <= requiredQuantity * 0.2) {
            return 'LOW_STOCK';
        }
        return 'AVAILABLE';
    }

    function calculateMaterials(productCode, quantity) {
        const templateItems = materialTemplates[productCode] || [];
        return templateItems.map(function(item) {
            const baseQuantity = Number(item.baseQuantity || item.requiredQuantity || 0);
            const availableStock = Number(item.availableStock || 0);
            const requiredQuantity = Number((baseQuantity * quantity).toFixed(2));
            const shortageQuantity = Number(Math.max(requiredQuantity - availableStock, 0).toFixed(2));
            const statusKey = calculateMaterialStatus(requiredQuantity, availableStock);

            return {
                materialCode: item.materialCode,
                materialName: item.materialName,
                requiredQuantity: requiredQuantity,
                availableStock: Number(availableStock.toFixed(2)),
                shortageQuantity: shortageQuantity,
                unit: item.unit,
                statusKey: statusKey,
                statusLabel: materialStatusMeta[statusKey].label,
                statusCssClass: materialStatusMeta[statusKey].cssClass,
                attentionRequired: statusKey !== 'AVAILABLE'
            };
        });
    }

    function updateProductCard() {
        const product = getSelectedProduct();
        const codeDisplay = document.getElementById('productCodeDisplay');
        if (!product) {
            codeDisplay.value = '';
            unitInput.value = '';
            productInfoEmpty.classList.remove('d-none');
            productInfoCard.classList.add('d-none');
            return;
        }

        codeDisplay.value = product.code || '';
        unitInput.value = product.defaultUnit || '';
        productInfoEmpty.classList.add('d-none');
        productInfoCard.classList.remove('d-none');
        document.getElementById('productInfoTitle').textContent = product.name || '-';
        document.getElementById('productInfoCode').textContent = product.code || '-';
        document.getElementById('productInfoGroup').textContent = product.productGroup || '-';
        document.getElementById('productInfoDuration').textContent = formatQuantity(product.standardProductionHours) + ' saat';
        document.getElementById('productInfoUnit').textContent = product.defaultUnit || '-';
        document.getElementById('productInfoBom').innerHTML = badge(product.bomAvailable ? 'Reçete Hazır' : 'Reçete Eksik', product.bomAvailable ? 'success' : 'danger');
    }

    function updateLineCard() {
        const line = getSelectedLine();
        if (!line) {
            lineInfoEmpty.classList.remove('d-none');
            lineInfoCard.classList.add('d-none');
            return;
        }

        const lineStatus = statusMeta[line.status] || statusMeta.AVAILABLE;
        lineInfoEmpty.classList.add('d-none');
        lineInfoCard.classList.remove('d-none');
        document.getElementById('lineInfoTitle').textContent = line.name || '-';
        document.getElementById('lineInfoCode').textContent = line.code || '-';
        document.getElementById('lineInfoCapacity').textContent = formatQuantity(line.capacity) + ' ' + (line.capacityUnit || '');
        document.getElementById('lineInfoStatus').innerHTML = badge(lineStatus.label, lineStatus.cssClass);
        document.getElementById('lineInfoTeam').textContent = line.responsibleTeam || '-';
        document.getElementById('lineInfoSuitability').innerHTML = badge(line.suitable ? 'Planlanabilir' : 'Kısıtlı', line.suitable ? 'success' : 'warning');
    }

    function renderMaterials() {
        const product = getSelectedProduct();
        const quantity = parseQuantity();
        const materials = product && quantity > 0 ? calculateMaterials(product.code, quantity) : [];

        materialBody.innerHTML = '';
        if (!materials.length) {
            if (materialEmpty) {
                materialEmpty.classList.remove('d-none');
            }
            materialAlert.classList.add('d-none');
            toggleMaterialActions(false, product, quantity);
            updateSummary(materials);
            return;
        }

        if (materialEmpty) {
            materialEmpty.classList.add('d-none');
        }
        materials.forEach(function(material) {
            const row = document.createElement('tr');
            if (material.attentionRequired) {
                row.classList.add('production-material-row--attention');
            }
            row.innerHTML = '
                <td>' + material.materialCode + '</td>
                <td>' + material.materialName + '</td>
                <td class="text-end">' + formatQuantity(material.requiredQuantity) + '</td>
                <td class="text-end">' + formatQuantity(material.availableStock) + '</td>
                <td class="text-end fw-semibold">' + formatQuantity(material.shortageQuantity) + '</td>
                <td>' + material.unit + '</td>
                <td>' + badge(material.statusLabel, material.statusCssClass) + '</td>
            ';
            materialBody.appendChild(row);
        });

        const shortageCount = materials.filter(material => material.shortageQuantity > 0).length;
        if (shortageCount > 0) {
            materialAlert.textContent = shortageCount + ' malzeme kaleminde eksik bulunuyor. İş emrini planlamadan önce MRP aksiyonunu değerlendirin.';
            materialAlert.classList.remove('d-none');
        } else if (materials.some(material => material.statusKey === 'LOW_STOCK')) {
            materialAlert.textContent = 'Malzeme karşılanıyor ancak bazı kalemler düşük stok seviyesinde. Satın alma aksiyonunu takip edin.';
            materialAlert.classList.remove('d-none');
        } else {
            materialAlert.classList.add('d-none');
        }

        toggleMaterialActions(shortageCount > 0, product, quantity);
        updateSummary(materials);
    }

    function toggleMaterialActions(hasShortage, product, quantity) {
        const href = product ? '/planning/mrp?productCode=' + encodeURIComponent(product.code) + '&quantity=' + encodeURIComponent(quantity || 0) : '/planning/mrp';
        [mrpAction, mrpActionInline].forEach(function(link) {
            if (!link) {
                return;
            }
            link.href = href;
            link.classList.toggle('d-none', !hasShortage || !product || quantity <= 0);
        });
        if (procurementPlaceholderAction) {
            procurementPlaceholderAction.classList.toggle('d-none', !hasShortage || !product || quantity <= 0);
        }
    }

    function updateSummary(materials) {
        const product = getSelectedProduct();
        const line = getSelectedLine();
        const quantity = parseQuantity();
        const start = startDateInput.value;
        const end = endDateInput.value;
        const estimatedHours = product ? Number(product.standardProductionHours || 0) * quantity : 0;

        document.getElementById('summaryProduct').textContent = product ? product.name : '-';
        document.getElementById('summaryQuantity').textContent = quantity > 0 ? formatQuantity(quantity) + ' ' + (unitInput.value || 'Adet') : '-';
        document.getElementById('summaryLine').textContent = line ? line.name : '-';
        document.getElementById('summaryStart').textContent = start ? formatDate(start) : '-';
        document.getElementById('summaryEnd').textContent = end ? formatDate(end) : '-';
        document.getElementById('summaryDuration').textContent = estimatedHours > 0 ? formatQuantity(estimatedHours) + ' saat' : '-';
        document.getElementById('summaryStatusHint').textContent = product && line ? 'Hazır' : 'Taslak';

        const summaryBadge = document.getElementById('summaryMaterialAvailability');
        const materialStatus = determineSummaryMaterialStatus(materials || []);
        summaryBadge.className = 'erp-badge erp-badge--' + materialStatus.cssClass;
        summaryBadge.textContent = materialStatus.label;
    }

    function determineSummaryMaterialStatus(materials) {
        if (!materials.length) {
            return { label: 'Hesaplanmadı', cssClass: 'neutral' };
        }
        if (materials.some(material => material.statusKey === 'CRITICAL')) {
            return { label: 'Kritik', cssClass: 'danger' };
        }
        if (materials.some(material => material.statusKey === 'MISSING')) {
            return { label: 'Eksik', cssClass: 'danger' };
        }
        if (materials.some(material => material.statusKey === 'LOW_STOCK')) {
            return { label: 'Düşük Stok', cssClass: 'warning' };
        }
        return { label: 'Yeterli', cssClass: 'success' };
    }

    function formatDate(value) {
        if (!value) {
            return '-';
        }
        const date = new Date(value + 'T00:00:00');
        if (Number.isNaN(date.getTime())) {
            return '-';
        }
        return new Intl.DateTimeFormat('tr-TR').format(date);
    }

    function validateDateRange() {
        if (!startDateInput.value || !endDateInput.value) {
            endDateInput.setCustomValidity('');
            return;
        }
        if (new Date(endDateInput.value) < new Date(startDateInput.value)) {
            endDateInput.setCustomValidity('Bitiş tarihi başlangıç tarihinden önce olamaz.');
        } else {
            endDateInput.setCustomValidity('');
        }
    }

    form.addEventListener('submit', function(event) {
        validateDateRange();
        if (!form.checkValidity()) {
            event.preventDefault();
            event.stopPropagation();
        }
        form.classList.add('was-validated');
    });

    productSelect.addEventListener('change', function() {
        updateProductCard();
        renderMaterials();
    });
    lineSelect.addEventListener('change', function() {
        updateLineCard();
        updateSummary(calculateMaterials(productSelect.value, parseQuantity()));
    });
    quantityInput.addEventListener('input', renderMaterials);
    startDateInput.addEventListener('change', function() {
        validateDateRange();
        updateSummary(calculateMaterials(productSelect.value, parseQuantity()));
    });
    endDateInput.addEventListener('change', function() {
        validateDateRange();
        updateSummary(calculateMaterials(productSelect.value, parseQuantity()));
    });

    updateProductCard();
    updateLineCard();
    renderMaterials();
})();