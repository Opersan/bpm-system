(function() {
    'use strict';

    const form = document.getElementById('purchaseOrderForm');
    if (!form) {
        return;
    }

    const body = document.getElementById('purchaseItemsBody');
    const addButton = document.getElementById('addPurchaseItem');
    const supplierSelect = document.getElementById('supplierId');
    const supplierInfoCard = document.getElementById('supplierInfoCard');
    const supplierInfoEmpty = document.getElementById('supplierInfoEmpty');
    const currencySelect = document.getElementById('currency');

    function formatCurrency(value) {
        return new Intl.NumberFormat('tr-TR', {
            style: 'currency',
            currency: currencySelect ? currencySelect.value : 'TRY'
        }).format(Number(value || 0));
    }

    function escapeHtml(value) {
        return String(value || '')
            .replace(/&/g, '&amp;')
            .replace(/</g, '&lt;')
            .replace(/>/g, '&gt;')
            .replace(/"/g, '&quot;')
            .replace(/'/g, '&#039;');
    }

    function updateSupplierInfo() {
        const option = supplierSelect.options[supplierSelect.selectedIndex];
        if (!option || !option.value) {
            supplierInfoCard.classList.add('d-none');
            supplierInfoEmpty.classList.remove('d-none');
            return;
        }

        supplierInfoEmpty.classList.add('d-none');
        supplierInfoCard.classList.remove('d-none');
        supplierInfoCard.innerHTML = `
            <div class="supplier-card__title">${escapeHtml(option.textContent)}</div>
            <div class="supplier-card__grid">
                <div><span>E-posta</span><strong>${escapeHtml(option.dataset.email || '-')}</strong></div>
                <div><span>Adres</span><strong>${escapeHtml(option.dataset.address || '-')}</strong></div>
            </div>
        `;
    }

    function updateRow(row) {
        const select = row.querySelector('.item-select');
        const option = select.options[select.selectedIndex];
        const quantity = Number(row.querySelector('.item-quantity').value || 0);
        const priceInput = row.querySelector('.item-price');
        const vat = Number(row.querySelector('.item-vat').value || 0);

        if (option && option.value) {
            row.querySelector('.item-code').textContent = option.dataset.code || '-';
            row.querySelector('.item-uom').textContent = option.dataset.uom || '-';
            if (!priceInput.dataset.touched) {
                priceInput.value = option.dataset.price || 0;
            }
        } else {
            row.querySelector('.item-code').textContent = '-';
            row.querySelector('.item-uom').textContent = '-';
        }

        const lineTotal = quantity * Number(priceInput.value || 0);
        row.querySelector('.item-line-total').textContent = formatCurrency(lineTotal + (lineTotal * vat / 100));
    }

    function updateSummary() {
        let subtotal = 0;
        let vatTotal = 0;
        let itemCount = 0;

        body.querySelectorAll('.purchase-item-row').forEach(function(row) {
            const selected = row.querySelector('.item-select').value;
            const quantity = Number(row.querySelector('.item-quantity').value || 0);
            const price = Number(row.querySelector('.item-price').value || 0);
            const vat = Number(row.querySelector('.item-vat').value || 0);
            const lineTotal = quantity * price;

            if (selected) {
                itemCount += 1;
            }

            subtotal += lineTotal;
            vatTotal += lineTotal * vat / 100;
            updateRow(row);
        });

        document.getElementById('summarySubtotal').textContent = formatCurrency(subtotal);
        document.getElementById('summaryVat').textContent = formatCurrency(vatTotal);
        document.getElementById('summaryGrandTotal').textContent = formatCurrency(subtotal + vatTotal);
        document.getElementById('summaryItemCount').textContent = itemCount;
        document.getElementById('summaryCurrency').textContent = currencySelect ? currencySelect.value : 'TRY';
    }

    function addRow() {
        const firstRow = body.querySelector('.purchase-item-row');
        const clone = firstRow.cloneNode(true);
        clone.querySelector('.item-select').value = '';
        clone.querySelector('.item-code').textContent = '-';
        clone.querySelector('.item-quantity').value = 1;
        clone.querySelector('.item-uom').textContent = '-';
        clone.querySelector('.item-price').value = 0;
        clone.querySelector('.item-price').dataset.touched = '';
        clone.querySelector('.item-vat').value = 20;
        clone.querySelector('.item-line-total').textContent = formatCurrency(0);
        body.appendChild(clone);
        updateSummary();
    }

    body.addEventListener('change', function(event) {
        if (event.target.classList.contains('item-select')) {
            const row = event.target.closest('.purchase-item-row');
            row.querySelector('.item-price').dataset.touched = '';
            updateSummary();
        }
    });

    body.addEventListener('input', function(event) {
        if (event.target.classList.contains('item-price')) {
            event.target.dataset.touched = 'true';
        }
        if (event.target.closest('.purchase-item-row')) {
            updateSummary();
        }
    });

    body.addEventListener('click', function(event) {
        const button = event.target.closest('.remove-purchase-item');
        if (!button) {
            return;
        }
        if (body.querySelectorAll('.purchase-item-row').length === 1) {
            return;
        }
        button.closest('.purchase-item-row').remove();
        updateSummary();
    });

    addButton.addEventListener('click', addRow);
    supplierSelect.addEventListener('change', updateSupplierInfo);
    currencySelect.addEventListener('change', updateSummary);

    updateSupplierInfo();
    updateSummary();
})();
