package com.mceteams.xiidays.utils.spectate;

import net.neoforged.fml.common.EventBusSubscriber;

import static com.mceteams.xiidays.XIIDaysManagerMod.MODID;

/**
 * Ce fichier est conservé pour compatibilité mais n'est plus utilisé.
 * Le switch entre coéquipiers se fait maintenant via les keybinds (flèches directionnelles)
 * définis dans SpectateKeybinds.java (client-side) et gérés par SpectatePackets.java.
 *
 * Flèche droite → Coéquipier suivant
 * Flèche gauche → Coéquipier précédent
 */
@EventBusSubscriber(modid = MODID)
public class SpectateInput {
    // Classe vide, fonctionnalité déplacée vers SpectateKeybinds.java
}