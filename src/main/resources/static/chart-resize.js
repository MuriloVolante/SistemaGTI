(function () {
    const MIN_W_PX = 340;
    const MIN_H_PX = 160;
    const MAX_H_PX = 700;
    let onSave = null;

    function slotId(slot) {
        return slot.dataset.chart || '';
    }

    function alvoAltura(slot) {
        return slot.querySelector('.donut-wrap, .chart-canvas-wrap');
    }

    function clamp(v, min, max) {
        return Math.min(max, Math.max(min, v));
    }

    function prepararSlot(slot) {
        if (slot.dataset.resizeReady) return;
        slot.dataset.resizeReady = '1';

        const hx = document.createElement('span');
        hx.className = 'chart-handle chart-handle-x';
        hx.title = 'Arraste para ajustar a largura';
        const hy = document.createElement('span');
        hy.className = 'chart-handle chart-handle-y';
        hy.title = 'Arraste para ajustar a altura';
        slot.appendChild(hx);
        slot.appendChild(hy);

        hx.addEventListener('mousedown', e => iniciarX(e, slot));
        hy.addEventListener('mousedown', e => iniciarY(e, slot));
    }

    function iniciarX(e, slot) {
        e.preventDefault();
        const grid   = slot.parentElement;
        const gridW  = grid.getBoundingClientRect().width;
        const startW = slot.getBoundingClientRect().width;
        const startX = e.clientX;
        const minPct = clamp(MIN_W_PX / gridW * 100, 10, 100);

        document.body.classList.add('is-resizing-x');

        function mover(ev) {
            const pct = clamp((startW + ev.clientX - startX) / gridW * 100, minPct, 100);
            slot.style.setProperty('--w', pct.toFixed(2));
        }
        function soltar() {
            document.removeEventListener('mousemove', mover);
            document.removeEventListener('mouseup', soltar);
            document.body.classList.remove('is-resizing-x');
            slot.dataset.userResized = '1';
            persistir(slot);
        }
        document.addEventListener('mousemove', mover);
        document.addEventListener('mouseup', soltar);
    }

    function iniciarY(e, slot) {
        e.preventDefault();
        const alvo   = alvoAltura(slot);
        if (!alvo) return;
        const startH = alvo.getBoundingClientRect().height;
        const startY = e.clientY;

        document.body.classList.add('is-resizing-y');

        function mover(ev) {
            const h = clamp(startH + ev.clientY - startY, MIN_H_PX, MAX_H_PX);
            alvo.style.height = Math.round(h) + 'px';
        }
        function soltar() {
            document.removeEventListener('mousemove', mover);
            document.removeEventListener('mouseup', soltar);
            document.body.classList.remove('is-resizing-y');
            slot.dataset.userResized = '1';
            persistir(slot);
        }
        document.addEventListener('mousemove', mover);
        document.addEventListener('mouseup', soltar);
    }

    function persistir(slot) {
        if (typeof onSave !== 'function') return;
        const alvo = alvoAltura(slot);
        onSave(slotId(slot), {
            w: slot.style.getPropertyValue('--w') || null,
            h: alvo && alvo.style.height ? alvo.style.height : null
        });
    }

    window.initChartResize = function (opts) {
        onSave = (opts && opts.onSave) || null;
        document.querySelectorAll('.chart-slot').forEach(prepararSlot);
    };

    window.aplicarLayoutChart = function (id, cfg) {
        const slot = document.querySelector(`.chart-slot[data-chart="${id}"]`);
        if (!slot || !cfg) return;
        if (cfg.w) slot.style.setProperty('--w', cfg.w);
        const alvo = alvoAltura(slot);
        if (alvo && cfg.h) alvo.style.height = cfg.h;
        slot.dataset.userResized = '1';
    };

    window.resetarLayoutChart = function () {
        document.querySelectorAll('.chart-slot').forEach(slot => {
            slot.style.removeProperty('--w');
            const alvo = alvoAltura(slot);
            if (alvo) alvo.style.removeProperty('height');
            delete slot.dataset.userResized;
        });
    };
})();