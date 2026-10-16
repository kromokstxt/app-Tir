// Lire une feuille de résultat prise en photo. Tout se passe sur le téléphone :
// la photo n'est envoyée nulle part (bibliothèque Tesseract, dans /ocr).
// Ce qui est lu est toujours montré au tireur pour qu'il vérifie avant de l'utiliser.
(function () {

    // Transforme le texte lu en coups.
    // - coupsProfonds = false : un coup par ligne, ses points (0 à 10) ; « 9.6 » donne 9.
    // - coupsProfonds = true  : le coup profond (0 à 100) ; « 9.6 » donne 96.
    // Un numéro de coup en début de ligne (1, 2, 3…) est ignoré, ainsi que les lignes
    // de total, de date ou d'heure. Renvoie les valeurs lues, ex. ['9', '10', 'M'].
    function lireFeuille(texte, coupsProfonds) {
        // 1) Les lignes utiles, découpées en mots. Seuls les nombres seuls comptent
        //    (« A100 » ou « 14:32 » ne sont pas des coups).
        const lignes = [];
        texte.split(/\r?\n/).forEach(function (ligne) {
            if (/tot|somme|sum|moy|\d[:/]\d|\d\.\d+\.\d/i.test(ligne)) return;
            const mots = ligne.trim().split(/\s+/);
            const nombres = mots.filter(function (m) { return /^\d+(?:[.,]\d)?$/.test(m); });
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

    // Devine si la feuille donne des coups profonds : la plupart des valeurs dépassent 10.
    function aDesCoupsProfonds(texte) {
        const profonds = lireFeuille(texte, true).filter(function (v) { return v !== 'M'; });
        const grands = profonds.filter(function (v) { return Number(v) > 10; });
        return profonds.length > 0 && grands.length * 2 > profonds.length;
    }

    // Prépare la photo : plus petite, en gris, plus contrastée (aide sur le papier thermique).
    function preparer(image) {
        const echelle = Math.min(1, 1600 / Math.max(image.width, image.height));
        const canvas = document.createElement('canvas');
        canvas.width = Math.round(image.width * echelle);
        canvas.height = Math.round(image.height * echelle);
        const ctx = canvas.getContext('2d');
        ctx.drawImage(image, 0, 0, canvas.width, canvas.height);
        const pixels = ctx.getImageData(0, 0, canvas.width, canvas.height);
        const d = pixels.data;
        for (let i = 0; i < d.length; i += 4) {
            const gris = 0.299 * d[i] + 0.587 * d[i + 1] + 0.114 * d[i + 2];
            const contraste = Math.max(0, Math.min(255, (gris - 128) * 1.6 + 128));
            d[i] = d[i + 1] = d[i + 2] = contraste;
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
            await worker.setParameters({ preserve_interword_spaces: '1' });
            const resultat = await worker.recognize(preparer(image));
            return resultat.data.text;
        } finally {
            await worker.terminate();
        }
    }

    const Scan = { lireFeuille: lireFeuille, aDesCoupsProfonds: aDesCoupsProfonds, lirePhoto: lirePhoto };
    if (typeof module !== 'undefined') module.exports = Scan;   // pour les essais
    if (typeof window !== 'undefined') window.Scan = Scan;
})();
