// Dessine la cible du club (comme la vraie : anneaux 1 à 4 blancs, 5 à 10 sur le noir)
// et place les coups d'une feuille de résultat dessus.
// La cible fait 100 de rayon : l'anneau 1 va jusqu'à 100, l'anneau 10 jusqu'à 10.
//
// Un coup s'écrit « 9>HD » (9 points, flèche en haut à droite), « 9:87>HD » (coup
// profond 87, donc 9 points), « 10 » (sans flèche) ou « M » (manqué).
// - La flèche donne la direction depuis le centre.
// - Le coup profond (100 = plein centre) donne la distance exacte au centre ;
//   sans lui, le coup est mis au milieu de son anneau.
(function () {
    const NS = 'http://www.w3.org/2000/svg';
    const MANQUE = 'M';
    const ANGLE_OR = 137.508 * Math.PI / 180;
    // Angle de chaque flèche (0 = vers la droite, l'axe vertical de l'écran va vers le bas).
    const ANGLES = { D: 0, BD: 45, B: 90, BG: 135, G: 180, HG: -135, H: -90, HD: -45 };
    const FLECHES = { H: '↑', HD: '↗', D: '→', BD: '↘', B: '↓', BG: '↙', G: '←', HG: '↖' };
    const MOTIF = /^(\d{1,2})(?::(\d{1,3}))?(?:>(H|HD|D|BD|B|BG|G|HG))?$/;

    function el(nom, attributs, parent) {
        const e = document.createElementNS(NS, nom);
        for (const [cle, valeur] of Object.entries(attributs)) e.setAttribute(cle, valeur);
        parent.appendChild(e);
        return e;
    }

    // « 9:87>HD » → { texte, points: 9, profond: 87, direction: 'HD' } ; null si illisible.
    function lireCoup(texte) {
        texte = texte.toUpperCase();
        if (texte === MANQUE) return { texte, manque: true };
        const m = MOTIF.exec(texte);
        if (!m || Number(m[1]) > 10 || (m[2] !== undefined && Number(m[2]) > 100)) return null;
        return { texte, points: Number(m[1]), profond: m[2] === undefined ? null : Number(m[2]), direction: m[3] || null };
    }

    // Mêmes points que côté serveur : 91 à 100 → 10, 81 à 90 → 9, …, 0 → 0.
    function pointsDepuisProfond(profond) {
        return Math.floor((profond + 9) / 10);
    }

    function lireCoups(texte) {
        return texte.trim().split(/\s+/).filter(c => c !== '').map(lireCoup).filter(c => c !== null);
    }

    function total(coups) {
        return coups.filter(c => !c.manque).reduce((somme, c) => somme + c.points, 0);
    }

    function totalProfond(coups) {
        return coups.filter(c => !c.manque).reduce((somme, c) => somme + (c.profond === null ? 0 : c.profond), 0);
    }

    // « 9 ↗ (87) »
    function decrire(coup) {
        if (coup.manque) return 'Manqué';
        return `${coup.points}${coup.direction ? ' ' + FLECHES[coup.direction] : ''}${coup.profond !== null ? ' (' + coup.profond + ')' : ''}`;
    }

    // Distance au centre. Le coup reste toujours dans l'anneau de ses points.
    function rayon(coup) {
        const [min, max] = coup.points === 10 ? [0, 10] : coup.points === 0 ? [100, 110] : [(10 - coup.points) * 10, (11 - coup.points) * 10];
        if (coup.profond === null) return coup.points === 10 ? 4 : (min + max) / 2;
        return Math.min(max - 0.5, Math.max(min + 0.5, 100 - coup.profond));
    }

    function dessiner(conteneur, coups) {
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

        const dejaVus = {};
        coups.forEach((coup, i) => {
            if (coup.manque) return;
            const r = rayon(coup);
            // Deux coups identiques sont un peu décalés pour qu'on voie les deux.
            const n = dejaVus[coup.texte] = (coup.texte in dejaVus ? dejaVus[coup.texte] : -1) + 1;
            const decalage = (n % 2 ? 1 : -1) * Math.ceil(n / 2) * 9 * Math.PI / 180;
            const a = (coup.direction ? ANGLES[coup.direction] * Math.PI / 180 : i * ANGLE_OR - Math.PI / 2) + decalage;
            const x = (r * Math.cos(a)).toFixed(1), y = (r * Math.sin(a)).toFixed(1);
            const g = el('g', { class: 'coup' }, svg);
            el('title', {}, g).textContent = `Coup ${i + 1} : ${decrire(coup)}`;
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
            if (cx === undefined) cx = vue.x + vue.taille / 2;
            if (cy === undefined) cy = vue.y + vue.taille / 2;
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

    window.Cible = { dessiner, lireCoup, lireCoups, total, totalProfond, decrire, pointsDepuisProfond, rendreZoomable, FLECHES, MANQUE };

    // Toutes les cibles de la page : <div data-cible data-coups="10>H 9:87>BG M">.
    document.addEventListener('DOMContentLoaded', () => {
        document.querySelectorAll('[data-cible]').forEach(c => {
            const svg = dessiner(c, lireCoups(c.dataset.coups || ''));
            if (c.hasAttribute('data-zoom')) c.zoom = rendreZoomable(svg);
        });
    });
})();
