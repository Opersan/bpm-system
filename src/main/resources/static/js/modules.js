// Modules JS - Handles logic for ERP Modules

$(document).ready(function() {
    
    // Initialize all DataTables with Turkish language
    $('.table').each(function() {
        if ($(this).find('tbody tr').length > 0 && !$(this).hasClass('no-datatable')) {
            $(this).DataTable({
                language: {
                    url: '//cdn.datatables.net/plug-ins/1.13.4/i18n/tr.json'
                },
                pageLength: 10,
                responsive: true
            });
        }
    });
    
    // --- Procurement Module ---

    // Create Order Form
    if ($('#createPoForm').length) {
        // Add Item Row
        $('#addItemBtn').click(function() {
            const row = $('#itemRowTemplate').html();
            $('#itemsContainer').append(row);
        });

        // Remove Item Row
        $(document).on('click', '.remove-item', function() {
            $(this).closest('.item-row').remove();
        });

        // Submit Form
        $('#createPoForm').submit(function(e) {
            e.preventDefault();
            
            const items = [];
            $('.item-row').each(function() {
                // Skip the template row if it's somehow selected (it shouldn't be as it's outside #itemsContainer)
                // But we are iterating .item-row inside #itemsContainer ideally.
                // The template is outside, but let's be safe.
                if ($(this).parent().attr('id') === 'itemRowTemplate') return;

                const itemId = $(this).find('[name="itemId"]').val();
                const quantity = $(this).find('[name="quantity"]').val();
                const price = $(this).find('[name="price"]').val();

                if (itemId && quantity && price) {
                    items.push({
                        itemId: itemId,
                        quantity: quantity,
                        price: price
                    });
                }
            });

            const data = {
                supplierId: $('#supplierId').val(),
                items: items
            };

            $.ajax({
                url: '/api/procurement/orders',
                type: 'POST',
                contentType: 'application/json',
                data: JSON.stringify(data),
                success: function(response) {
                    alert('Sipariş başarıyla oluşturuldu!');
                    window.location.href = '/procurement';
                },
                error: function(xhr) {
                    alert('Hata: ' + (xhr.responseJSON ? xhr.responseJSON.message : 'Bilinmeyen hata'));
                }
            });
        });
    }

    // --- Inventory Module ---

    // Receive Goods Form
    if ($('#receiveGoodsForm').length) {
        $('#addReceiveItemBtn').click(function() {
            const row = $('#receiveItemRowTemplate').html();
            $('#receiveItemsContainer').append(row);
        });

        $('#receiveGoodsForm').submit(function(e) {
            e.preventDefault();
            
            const items = [];
            $('.item-row').each(function() {
                if ($(this).parent().attr('id') === 'receiveItemRowTemplate') return;

                const itemId = $(this).find('[name="itemId"]').val();
                const quantity = $(this).find('[name="quantity"]').val();
                
                if (itemId && quantity) {
                    items.push({
                        itemId: itemId,
                        quantity: quantity
                    });
                }
            });

            const data = {
                purchaseOrderId: $('#purchaseOrderId').val(),
                warehouseId: $('#warehouseId').val(),
                items: items
            };

            $.ajax({
                url: '/api/inventory/receipts',
                type: 'POST',
                contentType: 'application/json',
                data: JSON.stringify(data),
                success: function(response) {
                    alert('Mal kabul işlemi başarıyla tamamlandı!');
                    window.location.href = '/inventory';
                },
                error: function(xhr) {
                    alert('Hata: ' + (xhr.responseJSON ? xhr.responseJSON.message : 'Bilinmeyen hata'));
                }
            });
        });
    }

    // --- Manufacturing Module ---

    // Create Work Order Form
    if ($('#createWorkOrderForm').length) {
        $('#createWorkOrderForm').submit(function(e) {
            e.preventDefault();
            
            const data = {
                itemId: $('#itemId').val(),
                quantity: $('#quantity').val(),
                startDate: $('#startDate').val(),
                endDate: $('#endDate').val()
            };

            $.ajax({
                url: '/api/manufacturing/work-orders',
                type: 'POST',
                contentType: 'application/json',
                data: JSON.stringify(data),
                success: function(response) {
                    alert('Work Order created successfully!');
                    window.location.href = '/manufacturing';
                },
                error: function(xhr) {
                    alert('Error creating work order: ' + xhr.responseText);
                }
            });
        });
    }

    // --- MRP Module ---

    // List MRP Runs
    if ($('#mrpRunsTable').length) {
        $('#mrpRunsTable').DataTable({
            ajax: {
                url: '/api/mrp/runs',
                dataSrc: ''
            },
            columns: [
                { data: 'id' },
                { data: 'runDate' },
                { data: 'horizonDays' },
                {
                    data: 'id',
                    render: function(data) {
                        return `<a href="#" class="btn btn-info btn-sm">View Planned Orders</a>`;
                    }
                }
            ]
        });
    }

    // Run MRP Form
    if ($('#runMrpForm').length) {
        $('#runMrpForm').submit(function(e) {
            e.preventDefault();
            
            const horizonDays = $('#horizonDays').val();

            $.ajax({
                url: '/api/mrp/run?horizonDays=' + horizonDays,
                type: 'POST',
                success: function(response) {
                    alert('MRP Run completed successfully!');
                    window.location.href = '/mrp';
                },
                error: function(xhr) {
                    alert('Error running MRP: ' + xhr.responseText);
                }
            });
        });
    }

    // --- Planning Module ---

    // New Item Form - Dynamic Summary Card Updates
    if ($('#code').length) {
        // Helper function to format price with Turkish Lira symbol
        function formatPrice(value) {
            if (!value || value === '') return '-';
            
            const number = parseFloat(value);
            if (isNaN(number)) return '-';
            
            // Format with 2 decimal places and Turkish Lira symbol
            return number.toLocaleString('tr-TR', { minimumFractionDigits: 2, maximumFractionDigits: 2 }) + ' ₺';
        }
        
        // Helper function to check if value is empty
        function isEmpty(value) {
            return value === null || value === undefined || value.trim() === '';
        }
        
        // Helper function to determine category from code and description
        function determineCategory(code, description) {
            const codeLower = (code || '').toLowerCase();
            const descLower = (description || '').toLowerCase();
            
            // Check for category keywords in code or description
            if (codeLower.includes('makine') || descLower.includes('makine') || descLower.includes('machine')) {
                return 'Makine';
            } else if (codeLower.includes('malzeme') || descLower.includes('malzeme') || descLower.includes('material')) {
                return 'Malzeme';
            } else if (codeLower.includes('parça') || descLower.includes('parça') || descLower.includes('part') || descLower.includes('piece')) {
                return 'Parça';
            } else if (codeLower.includes('ürün') || descLower.includes('ürün') || descLower.includes('product')) {
                return 'Ürün';
            } else if (codeLower.includes('ambalaj') || descLower.includes('ambalaj') || descLower.includes('packaging') || descLower.includes('package')) {
                return 'Ambalaj';
            } else {
                return 'Genel';
            }
        }
        
        // Update summary card
        function updateSummary() {
            // Update code
            if (isEmpty($('#code').val())) {
                $('#summary-code').text('-');
            } else {
                $('#summary-code').text($('#code').val());
            }
            
            // Update name
            if (isEmpty($('#name').val())) {
                $('#summary-name').text('-');
            } else {
                $('#summary-name').text($('#name').val());
            }
            
            // Update UOM
            if (isEmpty($('#uom').val())) {
                $('#summary-uom').text('-');
            } else {
                $('#summary-uom').text($('#uom').val());
            }
            
            // Update price
            $('#summary-price').text(formatPrice($('#price').val()));
            
            // Update category based on code and description
            $('#summary-category').text(determineCategory($('#code').val(), $('#description').val()));
        }
        
        // Add event listeners to all form fields
        $('#code').on('input', updateSummary);
        $('#name').on('input', updateSummary);
        $('#uom').on('change', updateSummary);
        $('#price').on('input', updateSummary);
        $('#description').on('input', updateSummary);
        
        // Initialize summary on page load (in case of validation errors and page reload)
        updateSummary();
    }

});

// Global Functions (outside document.ready to be accessible)

function approveOrder(id) {
    if(confirm('Are you sure you want to approve this order?')) {
        $.ajax({
            url: '/api/procurement/orders/' + id + '/approve',
            type: 'POST',
            success: function(response) {
                alert('Order approved!');
                $('#procurementTable').DataTable().ajax.reload();
            },
            error: function(xhr) {
                alert('Error approving order: ' + xhr.responseText);
            }
        });
    }
}

function updateWoStatus(id, status) {
    if(confirm('Update status to ' + status + '?')) {
        $.ajax({
            url: '/api/manufacturing/work-orders/' + id + '/status?status=' + status,
            type: 'PUT',
            success: function(response) {
                alert('Status updated!');
                $('#workOrderTable').DataTable().ajax.reload();
            },
            error: function(xhr) {
                alert('Error updating status: ' + xhr.responseText);
            }
        });
    }
}
