// Lire une feuille de résultat prise en photo. Tout se passe sur le téléphone :
// la photo n'est envoyée nulle part (bibliothèque Tesseract, dans /ocr).
// Ce qui est lu est toujours montré au tireur pour qu'il vérifie avant de l'utiliser.
(function () {

    // Transforme le texte lu en coups, un par ligne de la feuille, par ex. :
    //     Match
    //      1  ←  5   89          (numéro, flèche, points, coup profond)
    //      3  →  5x  96          (5x = 5 au centre)
    //     (3  →  5x  96)         (répétition entre parenthèses : ignorée)
    //     Subtotal: 20 361       (sous-totaux, total, en-tête, date : ignorés)
    // - coupsProfonds = false : les points (0 à 10) ; « 9.6 » donne 9.
    // - coupsProfonds = true  : le coup profond (0 à 100), le dernier nombre de la ligne ; « 9.6 » donne 96.
    // S'il y a une ligne « Match », les coups d'essai au-dessus (1P, 2P…) sont ignorés.
    // Renvoie les valeurs lues, ex. ['89', '96', 'M'].
    function lireFeuille(texte, coupsProfonds) {
        let toutes = texte.split(/\r?\n/);
        const match = toutes.findIndex(function (l) { return /^\W*match\b/i.test(l); });
        if (match >= 0) toutes = toutes.slice(match + 1);

        // 1) Les lignes de coups, découpées en mots nettoyés (« 2» » → « 2 », « 92) » → « 92 »).
        const lignes = [];
        toutes.forEach(function (ligne) {
            if (/[()]/.test(ligne)) return;                        // répétition d'un coup
            if (/\d[:/]\d|\d\.\d+\.\d/.test(ligne)) return;      // date ou heure
            const brut = ligne.trim().split(/\s+/);
            if (brut.some(function (m) { return /[A-Za-zÀ-ÿ]{3,}/.test(m); })) return;  // texte : en-tête, Subtotal, TOTAL…
            if (/^\d*[Pp]/.test(brut[0] || '')) return;              // coup d'essai (1P, 2P…)
            const mots = brut.map(function (m) { return m.replace(/^[^0-9A-Za-z]+|[^0-9A-Za-z.,]+$/g, ''); });
            const nombres = [];
            mots.forEach(function (m) {
                const n = /^(\d+(?:[.,]\d)?)[xX]?$/.exec(m);         // « 5x » compte comme 5
                if (n) nombres.push(n[1]);
            });
            const manque = mots.some(function (m) { return /^[Mm]$/.test(m); });
            if (nombres.length > 0 || manque) lignes.push({ nombres: nombres, manque: manque });
        });

        // 2) La feuille a-t-elle une colonne « numéro du coup » ? Oui si le premier nombre des lignes
        //    commence à 1, 2 ou 3 puis ne fait que monter (même si la photo a raté une ligne).
        const premiers = lignes.filter(function (l) { return l.nombres.length > 0; })
            .map(function (l) { return Number(l.nombres[0]); });
        let montees = 0;
        for (let i = 1; i < premiers.length; i++) {
            if (premiers[i] > premiers[i - 1]) montees++;
        }
        const colonneNumero = premiers.length > 1 && premiers[0] <= 3 && montees * 10 >= (premiers.length - 1) * 8;

        // 3) Une valeur par ligne.
        const valeurs = [];
        lignes.forEach(function (l) {
            const nombres = l.nombres.slice();
            if (colonneNumero && (nombres.length > 1 || l.manque)) nombres.shift();
            if (nombres.length === 0) {
                if (l.manque) valeurs.push('M');   // manqué
                return;
            }
            const decimal = nombres.find(function (n) { return /[.,]/.test(n); });
            let valeur;
            if (coupsProfonds) {
                // Une valeur à virgule (9.6) donne 96 ; au-delà de 100 (10.4), c'est le plein centre.
                valeur = decimal ? Math.min(100, Math.round(parseFloat(decimal.replace(',', '.')) * 10))
                                 : Number(nombres[nombres.length - 1]);
                if (valeur >= 0 && valeur <= 100) valeurs.push(String(valeur));
            } else {
                valeur = decimal ? Math.floor(parseFloat(decimal.replace(',', '.'))) : Number(nombres[0]);
                if (valeur >= 0 && valeur <= 10) valeurs.push(String(valeur));
            }
        });
        return valeurs;
    }

    // Les coups qui semblent mal lus, à vérifier : un coup profond sous 20 est rare
    // (souvent un chiffre perdu, « 91 » lu « 9 »). Renvoie leurs numéros (1, 2…).
    function aVerifier(valeurs, coupsProfonds) {
        const numeros = [];
        valeurs.forEach(function (v, i) {
            if (coupsProfonds && v !== 'M' && Number(v) < 20) numeros.push(i + 1);
        });
        return numeros;
    }

    // Devine si la feuille donne des coups profonds : la plupart des valeurs dépassent 10.
    function aDesCoupsProfonds(texte) {
        const profonds = lireFeuille(texte, true).filter(function (v) { return v !== 'M'; });
        const grands = profonds.filter(function (v) { return Number(v) > 10; });
        return profonds.length > 0 && grands.length * 2 > profonds.length;
    }

    // Trouve la bande de papier blanc (le ticket imprimé) dans la photo, pour ne lire qu'elle.
    // Renvoie null si on ne la trouve pas clairement (on lit alors toute la photo).
    function zoneDuPapier(image) {
        const echelle = Math.min(1, 800 / Math.max(image.width, image.height));   // petite copie : plus rapide
        const c = document.createElement('canvas');
        c.width = Math.round(image.width * echelle);
        c.height = Math.round(image.height * echelle);
        const g = c.getContext('2d');
        g.drawImage(image, 0, 0, c.width, c.height);
        const d = g.getImageData(0, 0, c.width, c.height).data;
        const parLigne = new Array(c.height).fill(0), parColonne = new Array(c.width).fill(0);
        for (let y = 0; y < c.height; y++) {
            for (let x = 0; x < c.width; x++) {
                const i = (y * c.width + x) * 4;
                const min = Math.min(d[i], d[i + 1], d[i + 2]), max = Math.max(d[i], d[i + 1], d[i + 2]);
                if (min > 170 && max - min < 40) { parLigne[y]++; parColonne[x]++; }   // blanc, pas coloré
            }
        }
        const bornes = function (comptes) {
            const seuil = Math.max.apply(null, comptes) * 0.5;
            const debut = comptes.findIndex(function (n) { return n > seuil; });
            let fin = comptes.length - 1;
            while (fin > debut && comptes[fin] <= seuil) fin--;
            return [debut, fin];
        };
        const [y0, y1] = bornes(parLigne), [x0, x1] = bornes(parColonne);
        const surface = (x1 - x0) * (y1 - y0) / (c.width * c.height);
        if (y0 < 0 || x0 < 0 || surface < 0.02 || surface > 0.9) return null;
        const marge = 4;
        return {
            x: Math.max(0, (x0 - marge) / echelle), y: Math.max(0, (y0 - marge) / echelle),
            l: Math.min(image.width, (x1 - x0 + 2 * marge) / echelle), h: Math.min(image.height, (y1 - y0 + 2 * marge) / echelle)
        };
    }

    // Seuil noir / blanc qui sépare le mieux l'encre du papier (méthode d'Otsu).
    function seuilOtsu(gris) {
        const histo = new Array(256).fill(0);
        gris.forEach(function (v) { histo[v]++; });
        const total = gris.length;
        let somme = 0;
        for (let i = 0; i < 256; i++) somme += i * histo[i];
        let sommeFond = 0, poidsFond = 0, meilleur = 0, seuil = 128;
        for (let t = 0; t < 256; t++) {
            poidsFond += histo[t];
            if (poidsFond === 0) continue;
            const poidsEncre = total - poidsFond;
            if (poidsEncre === 0) break;
            sommeFond += t * histo[t];
            const ecart = poidsFond * poidsEncre * Math.pow(sommeFond / poidsFond - (somme - sommeFond) / poidsEncre, 2);
            if (ecart > meilleur) { meilleur = ecart; seuil = t; }
        }
        return seuil;
    }

    // Prépare la photo pour la lecture : on garde le papier, on agrandit les petits chiffres,
    // puis on passe en noir et blanc.
    function preparer(image) {
        const zone = zoneDuPapier(image) || { x: 0, y: 0, l: image.width, h: image.height };
        const echelle = Math.min(4, 3000 / Math.max(zone.l, zone.h));
        const canvas = document.createElement('canvas');
        canvas.width = Math.round(zone.l * echelle);
        canvas.height = Math.round(zone.h * echelle);
        const ctx = canvas.getContext('2d');
        ctx.imageSmoothingQuality = 'high';
        ctx.drawImage(image, zone.x, zone.y, zone.l, zone.h, 0, 0, canvas.width, canvas.height);
        const pixels = ctx.getImageData(0, 0, canvas.width, canvas.height);
        const d = pixels.data;
        const gris = new Uint8Array(d.length / 4);
        for (let i = 0; i < gris.length; i++) {
            gris[i] = Math.round(0.299 * d[i * 4] + 0.587 * d[i * 4 + 1] + 0.114 * d[i * 4 + 2]);
        }
        const seuil = seuilOtsu(gris);
        for (let i = 0; i < gris.length; i++) {
            const v = gris[i] <= seuil ? 0 : 255;
            d[i * 4] = d[i * 4 + 1] = d[i * 4 + 2] = v;
        }
        ctx.putImageData(pixels, 0, 0);
        return canvas;
    }

    function chargerImage(fichier) {
        return new Promise(function (ok, erreur) {
            const image = new Image();
            image.onload = function () { ok(image); };
            image.onerror = erreur;
            image.src = URL.createObjectURL(fichier);
        });
    }

    function chargerTesseract(adresse) {
        if (window.Tesseract) return Promise.resolve();
        return new Promise(function (ok, erreur) {
            const script = document.createElement('script');
            script.src = adresse;
            script.onload = ok;
            script.onerror = erreur;
            document.head.appendChild(script);
        });
    }

    // Lit le texte d'une photo. chemins : adresses des fichiers de /ocr. avancement(0 à 100).
    async function lirePhoto(fichier, chemins, avancement) {
        await chargerTesseract(chemins.tesseract);
        const image = await chargerImage(fichier);
        const worker = await Tesseract.createWorker('eng', 1, {
            workerPath: chemins.worker,
            corePath: chemins.core,
            langPath: chemins.langues,
            gzip: true,
            logger: function (m) {
                if (m.status === 'recognizing text') avancement(Math.round(m.progress * 100));
            }
        });
        try {
            // Le ticket est une seule colonne de texte (mode 6 de Tesseract).
            await worker.setParameters({ preserve_interword_spaces: '1', tessedit_pageseg_mode: '6' });
            const resultat = await worker.recognize(preparer(image));
            return resultat.data.text;
        } finally {
            await worker.terminate();
        }
    }

    const Scan = { lireFeuille: lireFeuille, aVerifier: aVerifier, aDesCoupsProfonds: aDesCoupsProfonds, lirePhoto: lirePhoto };
    if (typeof module !== 'undefined') module.exports = Scan;   // pour les essais
    if (typeof window !== 'undefined') window.Scan = Scan;
})();
