/* CleanTrack - shared behaviour for the Inventory, Suppliers and Supply Orders pages.
   Search and filters, table/card toggle (remembered in the browser), stock bars,
   stock adjustment dialog, automatic-use form helper and confirmation dialogs. */
(function () {
    'use strict';

    var PREFIX = 'cleantrack.';
    function readPref(key) {
        try { return window.localStorage.getItem(PREFIX + key); } catch (e) { return null; }
    }
    function writePref(key, value) {
        try { window.localStorage.setItem(PREFIX + key, value); } catch (e) { /* storage unavailable */ }
    }
    function $(selector, root) { return (root || document).querySelector(selector); }
    function $all(selector, root) { return Array.prototype.slice.call((root || document).querySelectorAll(selector)); }
    function text(value) { return value === undefined || value === null ? '' : String(value); }

    /* ---------- Confirmation dialog (replaces the browser confirm box) ---------- */
    var confirmModal = null;
    var confirmAction = null;

    function buildConfirm() {
        if (confirmModal || !window.bootstrap) { return; }
        var wrapper = document.createElement('div');
        wrapper.innerHTML =
            '<div class="modal fade" id="ctConfirm" tabindex="-1" aria-hidden="true">' +
            '<div class="modal-dialog modal-dialog-centered modal-sm"><div class="modal-content text-center">' +
            '<div class="modal-body p-4">' +
            '<div class="confirm-icon" id="ctConfirmIcon"><i class="fa-solid fa-triangle-exclamation"></i></div>' +
            '<h5 class="mb-2" id="ctConfirmTitle">Please confirm</h5>' +
            '<p class="text-muted mb-4" id="ctConfirmMessage"></p>' +
            '<div class="d-flex gap-2 justify-content-center">' +
            '<button type="button" class="btn btn-outline-custom" data-bs-dismiss="modal">Cancel</button>' +
            '<button type="button" class="btn btn-danger-custom" id="ctConfirmOk">Confirm</button>' +
            '</div></div></div></div></div>';
        confirmModal = wrapper.firstChild;
        document.body.appendChild(confirmModal);
        $('#ctConfirmOk', confirmModal).addEventListener('click', function () {
            var action = confirmAction;
            confirmAction = null;
            window.bootstrap.Modal.getOrCreateInstance(confirmModal).hide();
            if (action) { action(); }
        });
    }

    function askConfirm(message, label, tone, onYes) {
        buildConfirm();
        if (!confirmModal) {
            if (window.confirm(message)) { onYes(); }
            return;
        }
        var ok = $('#ctConfirmOk', confirmModal);
        var icon = $('#ctConfirmIcon', confirmModal);
        $('#ctConfirmMessage', confirmModal).textContent = message;
        ok.textContent = label || 'Confirm';
        var accent = tone === 'accent';
        ok.className = accent ? 'btn btn-primary-custom' : 'btn btn-danger-custom';
        icon.className = 'confirm-icon' + (accent ? ' tone-accent' : '');
        icon.firstChild.className = accent ? 'fa-solid fa-circle-question' : 'fa-solid fa-triangle-exclamation';
        confirmAction = onYes;
        window.bootstrap.Modal.getOrCreateInstance(confirmModal).show();
    }

    document.addEventListener('submit', function (event) {
        var form = event.target;
        if (!form || !form.getAttribute || !form.hasAttribute('data-confirm')) { return; }
        if (form.getAttribute('data-confirmed') === '1') { return; }
        event.preventDefault();
        askConfirm(form.getAttribute('data-confirm'),
            form.getAttribute('data-confirm-label'),
            form.getAttribute('data-confirm-tone'),
            function () {
                form.setAttribute('data-confirmed', '1');
                form.submit();
            });
    });

    /* ---------- Success messages fade away ---------- */
    $all('.alert-success').forEach(function (alertBox) {
        window.setTimeout(function () {
            if (window.bootstrap && alertBox.isConnected) {
                try { window.bootstrap.Alert.getOrCreateInstance(alertBox).close(); } catch (e) { /* already closed */ }
            }
        }, 8000);
    });

    /* ---------- Stock bars ---------- */
    $all('.stock-bar').forEach(function (bar) {
        var qty = parseInt(bar.getAttribute('data-qty'), 10);
        var min = parseInt(bar.getAttribute('data-min'), 10);
        var fill = $('.stock-fill', bar);
        if (isNaN(qty) || isNaN(min) || !fill) { return; }
        var percent = min > 0 ? Math.min(100, Math.round((qty / (min * 2)) * 100)) : (qty > 0 ? 100 : 0);
        fill.style.width = Math.max(percent, qty > 0 ? 4 : 0) + '%';
        if (qty < min) { bar.className += ' low'; }
        else if (min > 0 && qty < min * 1.5) { bar.className += ' near'; }
    });

    /* ---------- Search, filters and table/card view ---------- */
    var pageKey = document.body.getAttribute('data-page') || 'page';
    var entries = $all('[data-filter-item]');
    var searchInput = $('#filterSearch');
    var categorySelect = $('#filterCategory');
    var chips = $all('[data-chip]');
    var viewButtons = $all('[data-set-view]');
    var viewBoxes = $all('[data-view]');
    var countLabel = $('#filterCount');
    var noResults = $('#noResults');
    var state = { query: '', category: '', chip: 'all', view: 'table' };
    var narrow = window.matchMedia ? window.matchMedia('(max-width: 991px)') : { matches: false };

    function activeView() {
        return viewBoxes.length ? (narrow.matches ? 'cards' : state.view) : null;
    }

    function applyFilters() {
        var query = state.query.toLowerCase();
        var view = activeView();
        var shown = 0;
        var total = 0;
        entries.forEach(function (entry) {
            var tags = (entry.getAttribute('data-tags') || '').split(' ');
            var category = entry.getAttribute('data-category') || '';
            var matches = true;
            if (query && entry.textContent.toLowerCase().indexOf(query) === -1) { matches = false; }
            if (state.category && category !== state.category) { matches = false; }
            if (state.chip !== 'all' && tags.indexOf(state.chip) === -1) { matches = false; }
            entry.hidden = !matches;
            var box = entry.closest('[data-view]');
            if (!box || box.getAttribute('data-view') === view) {
                total += 1;
                if (matches) { shown += 1; }
            }
        });
        if (countLabel) { countLabel.textContent = 'Showing ' + shown + ' of ' + total; }
        if (noResults) { noResults.hidden = !(total > 0 && shown === 0); }
        chips.forEach(function (chip) {
            chip.classList.toggle('active', chip.getAttribute('data-chip') === state.chip);
        });
        $all('[data-chip-tile]').forEach(function (tile) {
            tile.classList.toggle('is-active', state.chip !== 'all' && tile.getAttribute('data-chip-tile') === state.chip);
        });
    }

    function showView() {
        var view = narrow.matches ? 'cards' : state.view;
        viewBoxes.forEach(function (box) { box.hidden = box.getAttribute('data-view') !== view; });
        viewButtons.forEach(function (button) {
            button.classList.toggle('active', button.getAttribute('data-set-view') === view);
        });
        applyFilters();
    }

    function setView(view) {
        state.view = view;
        writePref('view.' + pageKey, view);
        showView();
    }

    if (categorySelect) {
        var seen = {};
        entries.forEach(function (entry) {
            var category = entry.getAttribute('data-category');
            if (category) { seen[category] = true; }
        });
        Object.keys(seen).sort().forEach(function (category) {
            var option = document.createElement('option');
            option.value = category;
            option.textContent = category;
            categorySelect.appendChild(option);
        });
        categorySelect.addEventListener('change', function () {
            state.category = categorySelect.value;
            applyFilters();
        });
    }
    if (searchInput) {
        searchInput.addEventListener('input', function () {
            state.query = searchInput.value.trim();
            applyFilters();
        });
    }
    chips.forEach(function (chip) {
        chip.addEventListener('click', function () {
            state.chip = chip.getAttribute('data-chip');
            applyFilters();
        });
    });
    $all('[data-chip-tile]').forEach(function (tile) {
        tile.addEventListener('click', function () {
            var value = tile.getAttribute('data-chip-tile');
            state.chip = state.chip === value ? 'all' : value;
            applyFilters();
        });
    });
    viewButtons.forEach(function (button) {
        button.addEventListener('click', function () { setView(button.getAttribute('data-set-view')); });
    });

    if (viewBoxes.length) {
        var saved = readPref('view.' + pageKey);
        if (saved !== 'table' && saved !== 'cards') { saved = 'table'; }
        state.view = saved;
        showView();
        if (narrow.addEventListener) { narrow.addEventListener('change', showView); }
    } else {
        applyFilters();
    }

    /* ---------- Adjust stock dialog (Use / Restock) ---------- */
    var adjustModal = $('#adjustModal');
    if (adjustModal) {
        var adjustForm = $('#adjustForm');
        var amountInput = $('#adjustAmount');
        var preview = $('#adjustPreview');
        var restockOption = $('#adjustRestockOption');
        var submitButton = $('#adjustSubmit');
        var current = { id: '', qty: 0, unit: '', name: '' };

        var currentMode = function () {
            var selected = $('input[name="adjustMode"]:checked', adjustModal);
            return selected ? selected.value : 'use';
        };

        var refreshAdjust = function () {
            var mode = currentMode();
            var base = mode === 'restock' ? adjustModal.getAttribute('data-restock') : adjustModal.getAttribute('data-consume');
            adjustForm.setAttribute('action', base + current.id);
            submitButton.innerHTML = mode === 'restock'
                ? '<i class="fa-solid fa-plus me-2"></i>Restock'
                : '<i class="fa-solid fa-minus me-2"></i>Use stock';
            var amount = parseInt(amountInput.value, 10);
            var unit = current.unit ? ' ' + current.unit : '';
            preview.className = 'adjust-preview';
            if (isNaN(amount) || amount < 1) {
                preview.textContent = 'Enter a quantity to see the new stock level.';
                return;
            }
            var result = mode === 'restock' ? current.qty + amount : current.qty - amount;
            if (result < 0) {
                preview.className = 'adjust-preview bad';
                preview.textContent = 'Not enough stock: only ' + current.qty + unit + ' available.';
            } else {
                preview.textContent = 'New stock level: ' + result + unit;
            }
        };

        adjustModal.addEventListener('show.bs.modal', function (event) {
            var trigger = event.relatedTarget;
            if (!trigger) { return; }
            current.id = trigger.getAttribute('data-id') || '';
            current.name = trigger.getAttribute('data-name') || '';
            current.unit = trigger.getAttribute('data-unit') || '';
            current.qty = parseInt(trigger.getAttribute('data-qty'), 10) || 0;
            var canRestock = trigger.getAttribute('data-manage') === 'true';
            restockOption.hidden = !canRestock;
            $('input[name="adjustMode"][value="use"]', adjustModal).checked = true;
            $('#adjustName').textContent = current.name;
            $('#adjustCurrent').textContent = current.qty + (current.unit ? ' ' + current.unit : '');
            amountInput.value = '';
            refreshAdjust();
        });
        adjustModal.addEventListener('shown.bs.modal', function () { amountInput.focus(); });
        amountInput.addEventListener('input', refreshAdjust);
        $all('input[name="adjustMode"]', adjustModal).forEach(function (radio) {
            radio.addEventListener('change', refreshAdjust);
        });
        $all('[data-add]', adjustModal).forEach(function (button) {
            button.addEventListener('click', function () {
                var amount = parseInt(amountInput.value, 10);
                amountInput.value = (isNaN(amount) ? 0 : amount) + parseInt(button.getAttribute('data-add'), 10);
                refreshAdjust();
            });
        });
        $('[data-clear]', adjustModal).addEventListener('click', function () {
            amountInput.value = '';
            refreshAdjust();
            amountInput.focus();
        });
    }

    /* ---------- Automatic use block in the add / edit forms ---------- */
    var serviceNames = { WASH_ONLY: 'Wash Only', WASH_IRON: 'Wash & Iron', DRY_CLEAN: 'Dry Clean' };
    $all('.usage-block').forEach(function (block) {
        var basis = $('select[name="usageBasis"]', block);
        var amount = $('input[name="usageAmount"]', block);
        var boxes = $all('input[name="usageServices"]', block);
        var extras = $all('.usage-extra', block);
        var summary = $('.usage-preview', block);
        if (!basis) { return; }

        var refresh = function () {
            var manual = basis.value === 'MANUAL' || basis.value === '';
            extras.forEach(function (extra) { extra.hidden = manual; });
            amount.disabled = manual;
            boxes.forEach(function (box) { box.disabled = manual; });
            if (!summary) { return; }
            if (manual) {
                summary.textContent = 'Manual only: the stock is reduced only when someone uses it.';
                return;
            }
            var n = parseInt(amount.value, 10);
            var chosen = boxes.filter(function (box) { return box.checked; }).map(function (box) {
                return serviceNames[box.value] || box.value;
            });
            var rule;
            if (isNaN(n) || n < 1) { rule = 'Enter an amount'; }
            else if (basis.value === 'PER_GARMENTS') { rule = '1 unit per ' + n + ' garments (rounded up)'; }
            else { rule = n + ' unit' + (n === 1 ? '' : 's') + ' per order'; }
            summary.textContent = rule + (chosen.length ? ' for ' + chosen.join(', ') : '. Tick at least one service.');
        };

        basis.addEventListener('change', refresh);
        amount.addEventListener('input', refresh);
        boxes.forEach(function (box) { box.addEventListener('change', refresh); });
        refresh();
    });
})();
