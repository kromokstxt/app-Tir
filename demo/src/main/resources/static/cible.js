// Dessine la cible du club (comme la vraie : anneaux 1 à 4 blancs, 5 à 10 sur le noir)
// et place les coups d'une feuille de résultat dessus.
// La cible fait 100 de rayon : l'anneau 1 va jusqu'à 100, l'anneau 10 jusqu'à 10.
// On ne connaît que la valeur d'un coup, pas où il a touché : chaque coup est mis dans
// son anneau, et les coups sont répartis tout autour pour ne pas se cacher.
(function () {
    const NS = 'http://www.w3.org/2000/svg';
    const MANQUE = 'M';
    const ANGLE_OR = 137.508 * Math.PI / 180;

    function el(nom, attributs, parent) {
        const e = document.createElementNS(NS, nom);
        for (const [cle, valeur] of Object.entries(attributs)) e.setAttribute(cle, valeur);
        parent.appendChild(e);
        return e;
    }

    // Distance au centre : 10 (ou 100) au centre, 0 juste en dehors de l'anneau 1.
    function rayon(valeur, echelle) {
        if (valeur === 0) return 105;
        if (echelle === 10) return (10 - valeur + 0.5) * 10;
        return Math.max(1, 100 - valeur + 0.5);
    }

    // « 10 9 M 8 » → ['10', '9', 'M', '8'] ; les coups impossibles sont ignorés.
    function lireCoups(texte, echelle) {
        return texte.trim().split(/[\s,;]+/).filter(c => c !== '').map(c => c.toUpperCase())
            .filter(c => c === MANQUE || (/^\d+$/.test(c) && Number(c) <= echelle))
            .map(c => c === MANQUE ? c : String(Number(c)));
    }

    function total(coups) {
        return coups.filter(c => c !== MANQUE).reduce((somme, c) => somme + Number(c), 0);
    }

    function dessiner(conteneur, echelle, coups) {
        conteneur.innerHTML = '';
        const svg = el('svg', { viewBox: '-115 -115 230 230', role: 'img', 'aria-label': 'Cible' }, conteneur);
        el('rect', { x: -115, y: -115, width: 230, height: 230, fill: '#fff' }, svg);

        for (const r of [100, 90, 80, 70]) el('circle', { r, fill: '#fff', stroke: '#000', 'stroke-width': 0.6 }, svg);
        el('circle', { r: 60, fill: '#000' }, svg);
        for (const r of [50, 40, 30, 20, 10]) el('circle', { r, fill: 'none', stroke: '#fff', 'stroke-width': 0.6 }, svg);
        el('circle', { r: 3, fill: 'none', stroke: '#fff', 'stroke-width': 0.5 }, svg);

        // Numéros 1 à 9 sur les quatre diagonales.
        for (let anneau = 1; anneau <= 9; anneau++) {
            const r = (10.5 - anneau) * 10;
            for (const degres of [45, 135, 225, 315]) {
                const a = degres * Math.PI / 180;
                const t = el('text', {
                    x: (r * Math.cos(a)).toFixed(1), y: (r * Math.sin(a)).toFixed(1),
                    fill: anneau >= 5 ? '#fff' : '#000', 'font-size': 6,
                    'text-anchor': 'middle', 'dominant-baseline': 'central', 'font-family': 'Arial, sans-serif'
                }, svg);
                t.textContent = anneau;
            }
        }

        coups.forEach((valeur, i) => {
            if (valeur === MANQUE) return;
            const r = rayon(Number(valeur), echelle);
            const a = i * ANGLE_OR - Math.PI / 2;
            const x = (r * Math.cos(a)).toFixed(1), y = (r * Math.sin(a)).toFixed(1);
            const g = el('g', { class: 'coup' }, svg);
            el('title', {}, g).textContent = `Coup ${i + 1} : ${valeur}`;
            el('circle', { cx: x, cy: y, r: 3.2, fill: '#e8312a', stroke: '#fff', 'stroke-width': 0.5 }, g);
            const t = el('text', {
                x, y, fill: '#fff', 'font-size': 3.2,
                'text-anchor': 'middle', 'dominant-baseline': 'central', 'font-family': 'Arial, sans-serif'
            }, g);
            t.textContent = i + 1;
        });
        return svg;
    }

    // Zoom (boutons, molette, pincer avec deux doigts) et déplacement en glissant.
    function rendreZoomable(svg) {
        const depart = { x: -115, y: -115, taille: 230 };
        let vue = { ...depart };
        const afficher = () => svg.setAttribute('viewBox', `${vue.x} ${vue.y} ${vue.taille} ${vue.taille}`);
        function zoomer(facteur, cx, cy) {
            const nouvelle = Math.min(230, Math.max(15, vue.taille * facteur));
            cx = cx ?? vue.x + vue.taille / 2;
            cy = cy ?? vue.y + vue.taille / 2;
            vue.x = cx - (cx - vue.x) * nouvelle / vue.taille;
            vue.y = cy - (cy - vue.y) * nouvelle / vue.taille;
            vue.taille = nouvelle;
            afficher();
        }
        function point(clientX, clientY) {
            const rect = svg.getBoundingClientRect();
            return { x: vue.x + (clientX - rect.left) / rect.width * vue.taille,
                     y: vue.y + (clientY - rect.top) / rect.height * vue.taille };
        }
        svg.addEventListener('wheel', e => {
            e.preventDefault();
            const p = point(e.clientX, e.clientY);
            zoomer(e.deltaY < 0 ? 0.8 : 1.25, p.x, p.y);
        }, { passive: false });

        const doigts = new Map();
        let ecart = null;
        svg.addEventListener('pointerdown', e => { doigts.set(e.pointerId, e); svg.setPointerCapture(e.pointerId); });
        svg.addEventListener('pointermove', e => {
            if (!doigts.has(e.pointerId)) return;
            const avant = doigts.get(e.pointerId);
            doigts.set(e.pointerId, e);
            const rect = svg.getBoundingClientRect();
            if (doigts.size === 1) {
                vue.x -= (e.clientX - avant.clientX) / rect.width * vue.taille;
                vue.y -= (e.clientY - avant.clientY) / rect.height * vue.taille;
                afficher();
            } else if (doigts.size === 2) {
                const [a, b] = [...doigts.values()];
                const nouvelEcart = Math.hypot(a.clientX - b.clientX, a.clientY - b.clientY);
                if (ecart) {
                    const p = point((a.clientX + b.clientX) / 2, (a.clientY + b.clientY) / 2);
                    zoomer(ecart / nouvelEcart, p.x, p.y);
                }
                ecart = nouvelEcart;
            }
        });
        const lacher = e => { doigts.delete(e.pointerId); if (doigts.size < 2) ecart = null; };
        svg.addEventListener('pointerup', lacher);
        svg.addEventListener('pointercancel', lacher);

        return { zoomer, recentrer: () => { vue = { ...depart }; afficher(); } };
    }

    window.Cible = { dessiner, lireCoups, total, rendreZoomable, MANQUE };

    // Toutes les cibles de la page : <div data-cible data-echelle="10" data-coups="10 9 M">.
    document.addEventListener('DOMContentLoaded', () => {
        document.querySelectorAll('[data-cible]').forEach(c => {
            const echelle = Number(c.dataset.echelle);
            const svg = dessiner(c, echelle, lireCoups(c.dataset.coups || '', echelle));
            if (c.hasAttribute('data-zoom')) c.zoom = rendreZoomable(svg);
        });
    });
})();
