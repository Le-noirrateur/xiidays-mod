from fpdf import FPDF
from datetime import datetime

class PDF(FPDF):
    def header(self):
        if self.page_no() > 1:
            self.set_font("Helvetica", "I", 8)
            self.set_text_color(140, 140, 140)
            self.cell(0, 6, "XII Days - Fiche technique v1.6.2", align="C")
            self.ln(6)

    def footer(self):
        self.set_y(-15)
        self.set_font("Helvetica", "I", 7)
        self.set_text_color(160, 160, 160)
        self.cell(0, 10, f"Page {self.page_no()}/{{nb}}", align="C")

    def ch1(self, text):
        self.set_font("Helvetica", "B", 15)
        self.set_text_color(20, 20, 70)
        self.cell(0, 11, text, new_x="LMARGIN", new_y="NEXT")
        self.set_draw_color(200, 170, 0)
        self.set_line_width(0.8)
        self.line(10, self.get_y(), 200, self.get_y())
        self.ln(4)

    def ch2(self, text):
        self.set_x(self.l_margin)
        self.set_font("Helvetica", "B", 12)
        self.set_text_color(40, 40, 120)
        self.cell(0, 9, text, new_x="LMARGIN", new_y="NEXT")
        self.ln(1)

    def ch3(self, text):
        self.set_x(self.l_margin)
        self.set_font("Helvetica", "B", 10)
        self.set_text_color(80, 80, 80)
        self.cell(0, 7, text, new_x="LMARGIN", new_y="NEXT")
        self.ln(1)

    def p(self, text):
        self.set_x(self.l_margin)
        self.set_font("Helvetica", "", 9)
        self.set_text_color(40, 40, 40)
        self.multi_cell(0, 4.8, text)
        self.ln(1.5)

    def code(self, text):
        self.set_x(self.l_margin)
        self.set_font("Courier", "", 7.8)
        self.set_text_color(50, 50, 50)
        self.set_fill_color(238, 238, 245)
        self.multi_cell(0, 4.3, text, fill=True)
        self.ln(2)

    def bullet(self, text, indent=8):
        self.set_x(self.l_margin + indent)
        self.set_font("Helvetica", "", 9)
        self.set_text_color(40, 40, 40)
        w = self.w - self.r_margin - (self.l_margin + indent)
        self.multi_cell(w, 4.8, f"- {text}")
        self.ln(0.8)

    def note(self, text):
        self.set_x(self.l_margin)
        self.set_font("Helvetica", "I", 8)
        self.set_text_color(100, 100, 100)
        self.set_fill_color(245, 245, 230)
        self.multi_cell(0, 4.5, f"Note : {text}", fill=True)
        self.ln(2)

    def cell2(self, label, value):
        self.set_x(self.l_margin)
        self.set_font("Helvetica", "B", 9)
        self.set_text_color(60, 60, 60)
        self.cell(40, 5.5, label)
        self.set_font("Helvetica", "", 9)
        self.set_text_color(40, 40, 40)
        self.cell(0, 5.5, value, new_x="LMARGIN", new_y="NEXT")
        self.ln(0.5)


pdf = PDF(orientation="P", unit="mm", format="A4")
pdf.alias_nb_pages()
pdf.set_auto_page_break(auto=True, margin=18)

# ============================================================
# PAGE DE GARDE
# ============================================================
pdf.add_page()
pdf.ln(30)
pdf.set_font("Helvetica", "B", 26)
pdf.set_text_color(20, 20, 70)
pdf.cell(0, 15, "XII DAYS", align="C", new_x="LMARGIN", new_y="NEXT")
pdf.set_font("Helvetica", "", 16)
pdf.set_text_color(60, 60, 60)
pdf.cell(0, 10, "Fiche technique exhaustive", align="C", new_x="LMARGIN", new_y="NEXT")
pdf.ln(5)
pdf.set_draw_color(200, 170, 0)
pdf.set_line_width(1)
pdf.line(60, pdf.get_y(), 150, pdf.get_y())
pdf.ln(10)
pdf.set_font("Helvetica", "", 10)
pdf.set_text_color(100, 100, 100)
pdf.cell(0, 7, "Version mod : 1.6.2", align="C", new_x="LMARGIN", new_y="NEXT")
pdf.cell(0, 7, "Minecraft 1.21.1 - NeoForge 21.1.219", align="C", new_x="LMARGIN", new_y="NEXT")
pdf.cell(0, 7, "Package : com.mceteams.xiidays", align="C", new_x="LMARGIN", new_y="NEXT")
pdf.cell(0, 7, f"Document genere le {datetime.now().strftime('%d/%m/%Y')}", align="C", new_x="LMARGIN", new_y="NEXT")
pdf.ln(15)
pdf.set_font("Helvetica", "I", 9)
pdf.set_text_color(140, 140, 140)
pdf.set_x(pdf.l_margin)
pdf.multi_cell(0, 5, "Ce document decrit le fonctionnement interne du mod XII Days : architecture, systemes de jeu, protocole reseau, commandes, objets, blocs et donnees persistantes.", align="C")

# ============================================================
# TABLE DES MATIÈRES
# ============================================================
pdf.add_page()
pdf.ch1("Table des matieres")
pdf.ln(2)

toc = [
    "1. Presentation generale",
    "2. Architecture du mod",
    "3. Cycle de jeu (Day Cycle)",
    "4. Gestion des equipes",
    "5. Systeme de points",
    "6. Système spectateur",
    "7. Core Maze",
    "8. Objets et blocs",
    "9. Commandes",
    "10. Protocole reseau",
    "11. Fichiers de donnees",
    "12. Systeme de restrictions",
    "13. Interface client",
    "14. Configuration",
]
for entry in toc:
    pdf.set_x(pdf.l_margin + 8)
    pdf.set_font("Helvetica", "", 10)
    pdf.set_text_color(40, 40, 120)
    pdf.cell(0, 6.5, entry, new_x="LMARGIN", new_y="NEXT")

# ============================================================
# 1. PRÉSENTATION GÉNÉRALE
# ============================================================
pdf.add_page()
pdf.ch1("1. Presentation generale")

pdf.ch2("1.1 Description")
pdf.p("XII Days est un mod Minecraft NeoForge (1.21.1) developpe par Fire Sparks Studio. Il implemente un evenement PvP multi-equipe en deux phases (preparation et combat) sur 12 jours. Le mod fournit des outils d'administration, un systeme de points, un mode spectateur avance, des puzzles de type Core Maze, et un systeme de restrictions d'items/blocs.")

pdf.ch2("1.2 Metadonnees")
pdf.cell2("Mod ID :", "xiidays")
pdf.cell2("Version :", "1.6.2")
pdf.cell2("Minecraft :", "1.21.1")
pdf.cell2("NeoForge :", "21.1.219")
pdf.cell2("Auteurs :", "Fire Sparks Studio, LEFORT Dylan")
pdf.cell2("License :", "GNU AGPLv3")
pdf.cell2("Package :", "com.mceteams.xiidays")

# ============================================================
# 2. ARCHITECTURE
# ============================================================
pdf.ch1("2. Architecture du mod")

pdf.ch2("2.1 Vue d'ensemble")
pdf.p("Le mod suit une architecture client-serveur NeoForge standard avec une couche de donnees persistante en JSON. Le code est organise en packages fonctionnels sous com.mceteams.xiidays.")

pdf.ch2("2.2 Structure des packages")
packages = [
    ("game/", "Logique de jeu : cycle jour/nuit, points, scoreboard, equipes, enigmes"),
    ("commands/", "Commandes /xiidays et sous-commandes"),
    ("network/", "Paquets reseau personnalises (5 C->S, 5 S->C)"),
    ("data/", "Couche de persistance JSON (TeamData, Stats, DayCycle, Restrictions)"),
    ("item/", "Items customs : CoreDestroyer, TotemRevivality, registre"),
    ("world/", "Blocks customs : CoreBlock, SpawnerBlock, entites de bloc associees"),
    ("screen/", "Interfaces graphiques : scoreboard, core maze, stats, end screen"),
    ("client/", "Evenements et rendu cote client"),
    ("spectator/", "Gestion du mode spectateur et des zones de free cam"),
    ("visual/", "Rendu BER, overlays, visualisation de zones"),
    ("player/", "Handlers d'evenements joueur (connexion, degats, cassage de blocs)"),
    ("restriction/", "Systeme d'autorisation d'items/blocs"),
    ("config/", "Configuration NeoForge"),
]
for pkg, desc in packages:
    pdf.bullet(f"{pkg} {desc}")

pdf.ch2("2.3 Bus d'evenements")
pdf.p("Le mod utilise deux bus d'evenements NeoForge :")
pdf.bullet("Mod Event Bus (FML) : lifecycle, enregistrement des paquets, key mappings, couches GUI")
pdf.bullet("NeoForge Event Bus : gameplay (mort, degats, cassage, clic, attaque, fabrication, cuisson, connexion, suivi)")

# ============================================================
# 3. CYCLE DE JEU
# ============================================================
pdf.add_page()
pdf.ch1("3. Cycle de jeu (Day Cycle)")

pdf.ch2("3.1 Principe")
pdf.p("Le mod decoupe l'evenement en 12 jours. Chaque jour est gere par DaysManager et persiste dans day_cycle.json (champs : in_progress, current_day). Un jour ne peut pas depasser 12.")

pdf.ch2("3.2 Phases")
pdf.p("Le jeu se divise en deux phases de 6 jours :")

pdf.ch3("Phase 1 - Preparation (jours 1-6)")
pdf.p("Les joueurs respawn automatiquement apres leur mort avec un delai croissant : delai = nombre_de_morts * 3 secondes (max 20 secondes). Les joueurs peuvent miner, construire, et se preparer pour la phase 2.")

pdf.ch3("Phase 2 - Combat (jours 7-12)")
pdf.p("Mode permadeath : les joueurs morts ne respawnent pas automatiquement. Deux exceptions permettent un retour : utilisation d'un Totem of Revivality par un coequipier, ou passage au jour suivant (si le c ur de leur equipe est toujours intact). Si le c ur de l'equipe est detruit, les joueurs morts sont definitivement bloques en mode FREE_SPECTATE.")

pdf.ch2("3.3 Demarrage d'un jour")
steps = [
    "Incrementation du compteur current_day",
    "Diffusion d'un compte a rebours 3-2-1 avec effets de cecite et d'obscurite via packet",
    "Teleportation de tous les joueurs vivants a leur spawn d'equipe",
    "Affichage d'un titre en haut de l'ecran (XII Days - Jour N)",
    "Au jour 1 : message de bienvenue avec les regles du jeu",
    "Apres le jour 6 (phase 2) : respawn des spectateurs dont le c ur d'equipe est en vie",
]
for s in steps:
    pdf.bullet(s)

pdf.ch2("3.4 Arret d'un jour")
pdf.p("L'arret du jour applique un effet d'obscurite et de cecite infini a tous les joueurs. En phase 1, tous les spectateurs sont respawn automatiquement.")

pdf.ch2("3.5 Commandes associees")
pdf.code("/xiidays day start\n/xiidays day stop\n/xiidays day status\n/xiidays day set <1-12>")

# ============================================================
# 4. GESTION DES EQUIPES
# ============================================================
pdf.add_page()
pdf.ch1("4. Gestion des equipes")

pdf.ch2("4.1 Structure")
pdf.p("Chaque equipe est representee par TeamData et stockee dans teams.json. Les equipes utilisent un auto-increment des IDs a partir d'un compteur global.")

pdf.ch2("4.2 Donnees par equipe")
fields = [
    "UUID unique de l'equipe",
    "Nom de l'equipe",
    "Liste des membres (UUID des joueurs)",
    "Statut elimine (boolean)",
    "Position finale (entier, ex. 1 pour vainqueur)",
    "Coordonnees du spawn d'equipe",
    "Coordonnees du c ur d'equipe",
    "Etat de destruction du c ur (boolean)",
    "Etat de resolution du Core Maze (boolean)",
    "Progression du Core Maze (entier, 0-3)",
    "Zone de Free Cam (FCZ) : min et max (Vec3)",
]
for f in fields:
    pdf.bullet(f)

pdf.ch2("4.3 Elimination")
pdf.p("Quand une equipe est eliminee, le systeme verifie s'il ne reste qu'une seule equipe en vie. Si c'est le cas, la partie se termine : l'equipe vainqueure est enregistree avec finalPosition=1, le jour est arret, et un paquet OpenEndScoreboardPacket diffuse le classement final et le MVP a tous les joueurs.")

pdf.ch2("4.4 Rehabilitation")
pdf.p("La rehabilitation d'une equipe annule son elimination et reinitialise le statut de vainqueur (finalPosition) de toutes les equipes. Cela permet de reprendre la partie apres une elimination erronee.")

pdf.ch2("4.5 Points d'equipe (TeamStatsData)")
pdf.p("Les statistiques cumulatives par equipe sont stockees dans team_stats.json : points, kills, kill_streak, max_kill_streak, pointgain, pointloss, blocks_mined, damage_dealt, damage_received, et un historique des 3 dernieres entrees de points.")

# ============================================================
# 5. SYSTEME DE POINTS
# ============================================================
pdf.add_page()
pdf.ch1("5. Systeme de points")

pdf.ch2("5.1 Gestionnaire (PointsManager)")
pdf.p("PointsManager gere l'attribution et le suivi des points. Les donnees sont persistees en JSON (team_stats.json pour les equipes, player_stats.json pour les joueurs). Chaque changement de points declenche un PointsChangedEvent sur le NeoForge Event Bus pour les mises a jour en temps reel du classement.")

pdf.ch2("5.2 Sources de points (PointType)")
sources = [
    ("KILL", "+50", "Le joueur tue un autre joueur"),
    ("DEATH", "-25", "Le joueur meurt"),
    ("MINING", "+5 a +100", "Minage de minerais (Diamond/Netherite=100, Emerald=75, Gold=50, Iron/Amethyst=25, Coal/Lapis/Redstone=10, Copper=5)"),
    ("FIRST_BLOOD", "+2000", "Premier kill de la partie (enum declare mais non branche)"),
    ("KILL_STREAK", "+25/+50/+100", "Streak >=3, >=5, >=10 kills sans mourir"),
    ("CRATE", "+75", "Ramassage de caisse (non implemente)"),
    ("TOTEM", "+200", "Utilisation d'un Totem of Revivality"),
    ("CORE_MAZE", "+300", "Resolution complete du Core Maze (3 enigmes)"),
]
for src, pts, desc in sources:
    pdf.bullet(f"{src} : {pts} - {desc}")

pdf.ch2("5.3 Points joueur (PlayerStatsData)")
pdf.p("Les statistiques individuelles sont stockees dans player_stats.json : team_points (points apportes a l'equipe) et deaths (nombre de morts). Les kills ne sont pas traques individuellement mais via le systeme de kill_streak.")

pdf.ch2("5.4 Scoreboard et classement")
pdf.p("ScoreboardManager maintient un classement trie des equipes par points. Il detecte les changements de rang avec des fleches (UP/DOWN) affichees 20 secondes. Les batailles sont detectees quand deux equipes adjacentes echangent leurs positions au moins 3 fois, affichant une icone d'epees. Les donnees de classement sont envoyees au client via OpenScoreboardPacket.")

# ============================================================
# 6. SYSTEME SPECTATEUR
# ============================================================
pdf.add_page()
pdf.ch1("6. Systeme spectateur")

pdf.ch2("6.1 Gestionnaire (SpectateManager)")
pdf.p("SpectateManager centralise la gestion des joueurs morts. Il est declenche par l'evenement LivingDeathEvent et bascule le joueur en mode spectateur.")

pdf.ch2("6.2 Modes spectateur")
modes = [
    ("TEAMMATE_WATCH", "Camera suit un coequipier vivant. Le joueur voit a travers les yeux de son coequipier."),
    ("BASE_SPECTATE", "Mode Adventure + invulnerable + noPhysics, confine dans la FCZ de l'equipe ou dans un rayon de 30 blocs autour du spawn. Le joueur peut voler, traverser les blocs (noPhysics), et est invisible."),
    ("FREE_SPECTATE", "Vrai mode spectateur vanilla. Le joueur est teleporte au spawn du monde et ne peut pas entrer dans les FCZ des autres equipes."),
]
for mode, desc in modes:
    pdf.bullet(f"{mode} : {desc}")

pdf.ch2("6.3 Respawn")
pdf.p("La methode respawnPlayer() retire l'etat spectateur, teleporte le joueur au spawn de son equipe, et lui donne vie pleine + faim pleine en mode Survival. En phase 1, les respawns sont automatiques avec un delai (deaths * 3s, max 20s) via TaskScheduler. En phase 2, le respawn necessite un Totem of Revivality ou le passage au jour suivant.")

pdf.ch2("6.4 Commutateur de cible spectateur")
pdf.p("Les fleches droite/gauche permettent de changer de coequipier a observer. Chaque pression envoie un paquet SpectateSwitchPayload au serveur avec la direction (true=suivant, false=precedent).")

pdf.ch2("6.5 Zones de Free Cam (FCZ)")
pdf.p("Les FCZ sont definies par equipe avec deux coins (min, max). Elles sont visualisables cote client via ZoneVisualizer qui dessine les aretes de la boite avec des particules colorees (8 couleurs d'equipe : Rouge, Bleu, Vert, Jaune, Rose, Cyan, Orange, Violet).")

# ============================================================
# 7. CORE MAZE
# ============================================================
pdf.add_page()
pdf.ch1("7. Core Maze")

pdf.ch2("7.1 Principe")
pdf.p("Le Core Maze est un mini-jeu de 3 enigmes a resoudre pour detruire le c ur d'une equipe ennemie. Il est accessible en faisant un clic droit sur un bloc de c ur adverse avec un Core Destroyer.")

pdf.ch2("7.2 Core Destroyer")
pdf.p("L'item Core Destroyer (rarete Rare, 3 durabilite) est l'outil necessaire pour ouvrir le Core Maze. Un clic droit sur un bloc team_core adverse envoie un paquet CoreMazeOpenPacket au client qui ouvre l'ecran CoreMazeScreen.")

pdf.ch2("7.3 Enigmes (EnigmaGenerator)")
pdf.p("Le generateur produit 3 enigmes aleatoires parmi les types suivants :")
enigmas = [
    "Enigmes mathematiques (operations, equations simples)",
    "Suites logiques (trouver le prochain nombre)",
    "Enigmes de codage/dechiffrage",
    "Questions a choix sur le theme Minecraft",
    "Puzzles de mots lies a Minecraft",
]
for e in enigmas:
    pdf.bullet(e)
pdf.p("Les enigmes sont envoyees au client dans CoreMazeOpenPacket et affichees dans une interface avec une zone de saisie (EditBox).")

pdf.ch2("7.4 Deroulement")
pdf.p("Le joueur saisit ses reponses dans l'EditBox. Les reponses correctes sont detectees automatiquement et envoyees au serveur via CoreMazeAnswerPacket (contenant teamId, enigmaIndex, correct). Le serveur suit la progression dans maze_progress (0-3). Apres 3 reponses correctes, core_maze_solved = true, 300 points sont attribues a l'equipe, et la team notifiee. Le maze est bloque si deja en cours (maze_progress > 0) pour eviter les tentatives concurrentes.")

# ============================================================
# 8. OBJETS ET BLOCS
# ============================================================
pdf.add_page()
pdf.ch1("8. Objets et blocs")

pdf.ch2("8.1 Objets enregistres")
items = [
    ("team_core", "Epic", "Block item pour placer le bloc de c ur d'equipe"),
    ("team_spawner", "Epic", "Block item pour placer le bloc de spawn d'equipe"),
    ("core_destroyer", "Rare", "Outil pour ouvrir le Core Maze (3 durabilite)"),
    ("totem_revivalite", "Rare", "Permet de ressusciter un coequipier mort (stackable x16)"),
]
pdf.set_x(pdf.l_margin)
pdf.set_font("Helvetica", "B", 9)
pdf.set_text_color(40, 40, 40)
w = pdf.w - pdf.l_margin - pdf.r_margin
col_w = [40, 20, w - 60]
pdf.cell(col_w[0], 6, "Nom registre", 1)
pdf.cell(col_w[1], 6, "Rarete", 1)
pdf.cell(col_w[2], 6, "Description", 1, new_x="LMARGIN", new_y="NEXT")
pdf.set_font("Helvetica", "", 9)
for name, rarity, desc in items:
    pdf.set_x(pdf.l_margin)
    pdf.cell(col_w[0], 5.5, name, 1)
    pdf.cell(col_w[1], 5.5, rarity, 1)
    pdf.cell(col_w[2], 5.5, desc, 1, new_x="LMARGIN", new_y="NEXT")
pdf.ln(3)

pdf.ch2("8.2 Blocs")
blocks = [
    ("team_core", "Bloc de c ur d'equipe. Resistance 20F/1200F. Pas de loot. Immune aux pistons. Pas d'occlusion. Stocke teamId, puzzleSolved, puzzleStatus dans son BlockEntity."),
    ("team_spawner", "Bloc de spawn d'equipe. Invisible, sans collision. Resistance 20F/1200F. Pas de loot. Pas de spawn de mobs. Stocke teamId dans son BlockEntity. Rendu via BER (TeamSpawnerRenderer) visible uniquement si le joueur tient l'item ou un debug stick."),
]
for name, desc in blocks:
    pdf.bullet(f"{name} : {desc}")

pdf.ch2("8.3 Entites de bloc")
pdf.p("Deux BlockEntities sont enregistrees : team_core (CoreBlockEntity) avec les champs teamId (int), puzzleSolved (bool), puzzleStatus (int) ; et team_spawn (SpawnerBlockEntity) avec teamId (int).")

pdf.ch2("8.4 Onglets creatifs")
pdf.bullet("totem_revivalite dans CREATIVE_MODE_TAB.INGREDIENTS")
pdf.bullet("core_destroyer dans CREATIVE_MODE_TAB.TOOLS_AND_UTILITIES")

# ============================================================
# 9. COMMANDES
# ============================================================
pdf.add_page()
pdf.ch1("9. Commandes")

pdf.p("Toutes les commandes sont sous /xiidays (alias /xd). Necessitent permission niveau 4 (operateur) sauf indication contraire.")

pdf.ch2("9.1 /xiidays day")
pdf.code("/xiidays day start       # Demarre un nouveau jour\n/xiidays day stop        # Arrete le jour en cours\n/xiidays day status      # Affiche l'etat actuel\n/xiidays day set <1-12>  # Definit le jour manuellement")

pdf.ch2("9.2 /xiidays team")
pdf.code("/xiidays team create <nom>       # Cree une equipe\n/xiidays team remove <nom>        # Supprime une equipe\n/xiidays team add <team> <joueur>  # Ajoute un joueur\n/xiidays team remove member <joueur>  # Retire un joueur\n/xiidays team spawn <team>         # Positionne le spawn\n/xiidays team core <team>          # Positionne le coeur\n/xiidays team eliminate <team>     # Elimine une equipe\n/xiidays team revive <team>        # Rehabilite une equipe\n/xiidays team list                 # Liste les equipes")

pdf.ch2("9.3 /xiidays spec")
pdf.code("/xiidays spec respawn <joueur>  # Respawn un spectateur\n/xiidays spec respawnall          # Respawn tous les spectateurs\n/xiidays spec list                # Liste les spectateurs")

pdf.ch2("9.4 /xiidays zone")
pdf.code("/xiidays zone set <team> <pos1> <pos2>  # Definit la FCZ\n/xiidays zone remove <team>                 # Supprime la FCZ\n/xiidays zone show [team]                   # Affiche la zone\n/xiidays zone hide                          # Cache la zone")

pdf.ch2("9.5 /xiidays restrict")
pdf.code("/xiidays restrict status                       # Etat des restrictions\n/xiidays restrict items allow                # Autorise l'item en main\n/xiidays restrict items disallow             # Interdit l'item en main\n/xiidays restrict blocks allow               # Autorise le bloc vise\n/xiidays restrict blocks disallow            # Interdit le bloc vise\n/xiidays restrict bypass <joueur> on|off     # Bypass admin")

pdf.ch2("9.6 /xiidays data")
pdf.code("/xiidays data reload    # Recharge les donnees depuis le disque\n/xiidays data save       # Sauvegarde les donnees sur le disque\n/xiidays data domains    # Liste les domaines de donnees")

pdf.ch2("9.7 /xiidays score")
pdf.p("Ouvre le tableau des scores (accessible a tous les joueurs, pas de permission requise). Equivalent a la touche U.")

# ============================================================
# 10. PROTOCOLE RESEAU
# ============================================================
pdf.add_page()
pdf.ch1("10. Protocole reseau")

pdf.ch2("10.1 Version")
pdf.p("Le protocole utilise la version 1.0.0, enregistree via RegisterPayloadHandlersEvent. La direction est play (jeu) et le protocole est sans confirmation (noAcknowledgement).")

pdf.ch2("10.2 Paquets client -> serveur (5)")
client_packets = [
    ("RequestScoreboardPacket", "request_scoreboard", "Vide", "Le joueur demande le classement"),
    ("CoreMazeAnswerPacket", "core_maze_answer", "teamId, enigmaIndex, correct", "Soumission d'une reponse d'enigme"),
    ("RequestTeamStatsPacket", "request_team_stats", "teamName", "Demande de stats detaillees d'une equipe"),
    ("SpectateSwitchPayload", "spectate_switch", "direction (bool)", "Changement de cible spectateur"),
]
pdf.set_x(pdf.l_margin)
pdf.set_font("Helvetica", "B", 8.5)
pdf.set_text_color(40, 40, 40)
w = pdf.w - pdf.l_margin - pdf.r_margin
cw = [40, 35, w - 75]
pdf.cell(cw[0], 6, "Paquet", 1)
pdf.cell(cw[1], 6, "Canal", 1)
pdf.cell(cw[2], 6, "Contenu", 1, new_x="LMARGIN", new_y="NEXT")
pdf.set_font("Helvetica", "", 8.5)
for name, chan, payload, desc in client_packets:
    pdf.set_x(pdf.l_margin)
    pdf.set_font("Helvetica", "B", 8.5)
    pdf.cell(cw[0], 5.5, name, 1)
    pdf.set_font("Helvetica", "", 8.5)
    pdf.cell(cw[1], 5.5, chan, 1)
    pdf.cell(cw[2], 5.5, payload, 1, new_x="LMARGIN", new_y="NEXT")
    pdf.set_x(pdf.l_margin + cw[0] + cw[1])
    pdf.set_font("Helvetica", "I", 8)
    pdf.set_text_color(80, 80, 80)
    pdf.cell(cw[2], 5, desc, new_x="LMARGIN", new_y="NEXT")
    pdf.set_text_color(40, 40, 40)
pdf.ln(3)

pdf.ch2("10.3 Paquets serveur -> client (5)")
server_packets = [
    ("OpenScoreboardPacket", "open_scoreboard", "Liste TeamData, playerTeam", "Ouvre l'ecran de scoreboard"),
    ("CoreMazeOpenPacket", "core_maze_open", "Liste EnigmaPayload, teamId", "Ouvre l'ecran du Core Maze"),
    ("OpenTeamStatsPacket", "open_team_stats", "teamStats, playerStats", "Ouvre l'ecran des stats d'equipe"),
    ("OpenEndScoreboardPacket", "open_end_scoreboard", "winningTeamName, mvp, teamStats", "Ouvre l'ecran de fin de partie"),
    ("PointsPopupPayload", "points_popup", "points, typeName", "Affiche un popup de points"),
]
pdf.set_x(pdf.l_margin)
pdf.set_font("Helvetica", "B", 8.5)
pdf.set_text_color(40, 40, 40)
pdf.cell(cw[0], 6, "Paquet", 1)
pdf.cell(cw[1], 6, "Canal", 1)
pdf.cell(cw[2], 6, "Contenu", 1, new_x="LMARGIN", new_y="NEXT")
for name, chan, payload, desc in server_packets:
    pdf.set_x(pdf.l_margin)
    pdf.set_font("Helvetica", "B", 8.5)
    pdf.cell(cw[0], 5.5, name, 1)
    pdf.set_font("Helvetica", "", 8.5)
    pdf.cell(cw[1], 5.5, chan, 1)
    pdf.cell(cw[2], 5.5, payload, 1, new_x="LMARGIN", new_y="NEXT")
    pdf.set_x(pdf.l_margin + cw[0] + cw[1])
    pdf.set_font("Helvetica", "I", 8)
    pdf.set_text_color(80, 80, 80)
    pdf.cell(cw[2], 5, desc, new_x="LMARGIN", new_y="NEXT")
    pdf.set_text_color(40, 40, 40)
pdf.ln(3)

pdf.ch2("10.4 Serialisation")
pdf.p("Tous les paquets utilisent le systeme CustomPacketPayload de NeoForge avec StreamCodec<FriendlyByteBuf, ...> pour la serialisation binaire. Le paquet PointsPopupPayload utilise un codec center (champ String size pour le nom du type de points). La classe centrale PacketHandler enregistre tous les types de paquets et leurs gestionnaires.")

# ============================================================
# 11. FICHIERS DE DONNEES
# ============================================================
pdf.add_page()
pdf.ch1("11. Fichiers de donnees")

pdf.ch2("11.1 DataManager")
pdf.p("DataManager est la couche de persistance. Les donnees sont stockees dans <world>/xiidaysdata/ sous forme de fichiers JSON. Chaque fichier est associe a un domaine (domain) et est gere par une classe de donnees dediee.")

pdf.ch2("11.2 Fichiers")
files = [
    ("teams.json", "TeamDataManager", "Liste des equipes (nom, membres, configuration, FCZ, etat du core/maze)"),
    ("team_stats.json", "TeamStatsDataManager", "Statistiques cumulatives par equipe (points, kills, streaks, minerais, degats, historique)"),
    ("player_stats.json", "PlayerStatsDataManager", "Statistiques individuelles (team_points, deaths)"),
    ("day_cycle.json", "DayCycleDataManager", "Etat du cycle de jeu (in_progress, current_day)"),
    ("restrictions.json", "RestrictionsDataManager", "Liste d'autorisation des items et blocs (resource key -> boolean)"),
]
pdf.set_x(pdf.l_margin)
pdf.set_font("Helvetica", "B", 8.5)
pdf.set_text_color(40, 40, 40)
cw2 = [35, 40, w - 75]
pdf.cell(cw2[0], 6, "Fichier", 1)
pdf.cell(cw2[1], 6, "Classe", 1)
pdf.cell(cw2[2], 6, "Contenu", 1, new_x="LMARGIN", new_y="NEXT")
pdf.set_font("Helvetica", "", 8.5)
for name, cls, desc in files:
    pdf.set_x(pdf.l_margin)
    pdf.cell(cw2[0], 5.5, name, 1)
    pdf.cell(cw2[1], 5.5, cls, 1)
    pdf.cell(cw2[2], 5.5, desc, 1, new_x="LMARGIN", new_y="NEXT")
pdf.ln(3)

pdf.ch2("11.3 Fonctionnement")
pdf.p("Les donnees sont chargees au premier acces (lazy loading) et persiste immediatement lors des modifications (write-through). Le chemin du repertoire de donnees est recalcule a chaque acces avec verification du changement de serveur, eliminant les bugs de cache de chemin.")

# ============================================================
# 12. SYSTEME DE RESTRICTIONS
# ============================================================
pdf.add_page()
pdf.ch1("12. Systeme de restrictions")

pdf.ch2("12.1 Principe")
pdf.p("Le systeme de restrictions permet de controler les items et blocs que les joueurs peuvent utiliser. Il fonctionne sur le principe d'une allowlist : tous les items/blocs sont autorises par defaut, seuls ceux explicitement passes a false sont interdits.")

pdf.ch2("12.2 Points d'interception")
points = [
    "Ramasage d'item (ItemEntityPickupEvent.Pre)",
    "Placement de bloc (BlockEvent.EntityPlaceEvent)",
    "Cassage de bloc (BlockEvent.BreakEvent)",
    "Clic droit sur item (PlayerInteractEvent.RightClickItem)",
    "Attaque d'entite (AttackEntityEvent)",
    "Fabrication d'item (PlayerEvent.ItemCraftedEvent - rend aussi les ingredients)",
    "Cuisson d'item (PlayerEvent.ItemSmeltedEvent)",
    "Connexion joueur (inventaire parcouru)",
    "Suivi joueur (inventaire parcouru)",
    "Evenements de voisinage de bloc (BlockEvent.NeighborNotifyEvent)",
]
for p in points:
    pdf.bullet(p)

pdf.ch2("12.3 Bypass")
pdf.p("Les joueurs avec le bypass actif ignorent toutes les restrictions. Le bypass est active/desactive par commande (/xiidays restrict bypass <joueur> on|off) et persiste par joueur.")

pdf.ch2("12.4 Stockage")
pdf.p("Les restrictions sont stockees dans restrictions.json avec deux sections : items (ResourceLocation -> boolean) et blocks (ResourceLocation -> boolean).")

# ============================================================
# 13. INTERFACE CLIENT
# ============================================================
pdf.add_page()
pdf.ch1("13. Interface client")

pdf.ch2("13.1 Ecrans")
screens = [
    ("ScoreboardScreen", "Touche U ou /xiidays score", "Classement en temps reel avec medailles, points, c ur (vie/mort), fleches de changement de rang, icones de bataille. Scrollable. Cliquer sur son equipe ouvre TeamStatsScreen."),
    ("CoreMazeScreen", "Clic droit sur c ur adverse avec Core Destroyer", "3 enigmes avec EditBox, indicateurs de progression, aide (touche H), soumission automatique."),
    ("TeamStatsScreen", "Clic sur son equipe dans le scoreboard", "Stats detaillees : points, kills, deaths, K/D, minerais, degats infliges/recus, gains/pertes de points, tableau des joueurs."),
    ("EndScoreboardScreen", "Fin de partie (1 equipe restante)", "Banniere du vainqueur, MVP, classement final complet avec stats par equipe. Scrollable."),
]
for name, trigger, desc in screens:
    pdf.bullet(f"{name} ({trigger}) : {desc}")

pdf.ch2("13.2 HUD - Popups de points")
pdf.p("PointsPopupClientHandler affiche des popups animes en bas a droite de l'ecran. Chaque popup montre +X ou -X avec le nom de la source (KILL, DEATH, MINING...). Les popups sont codes par couleur (vert=gain, rouge=perte). Jusqu'a 5 popups peuvent etre empiles avec fondu progressif. La duree d'affichage augmente avec le nombre de popups (base 3s + 1.5s par popup supplementaire, max 10s). L'animation utilise un lissage exponentiel (visualY += (target - visualY) * 0.12).")

pdf.ch2("13.3 Touches")
kb = [
    ("U", "key.xiidays.open_scoreboard", "Ouvre le scoreboard"),
    ("Fleche droite", "key.xiidays.spectate_next", "Spectateur : joueur suivant"),
    ("Fleche gauche", "key.xiidays.spectate_previous", "Spectateur : joueur precedent"),
]
pdf.set_x(pdf.l_margin)
pdf.set_font("Helvetica", "B", 8.5)
cw3 = [30, 50, w - 80]
pdf.cell(cw3[0], 6, "Touche", 1)
pdf.cell(cw3[1], 6, "Nom registre", 1)
pdf.cell(cw3[2], 6, "Action", 1, new_x="LMARGIN", new_y="NEXT")
pdf.set_font("Helvetica", "", 8.5)
for key, name, action in kb:
    pdf.set_x(pdf.l_margin)
    pdf.cell(cw3[0], 5.5, key, 1)
    pdf.cell(cw3[1], 5.5, name, 1)
    pdf.cell(cw3[2], 5.5, action, 1, new_x="LMARGIN", new_y="NEXT")
pdf.ln(3)

pdf.ch2("13.4 Rendu de blocs")
pdf.p("TeamSpawnerRenderer est un BlockEntityRenderer (BER) qui affiche le bloc Spawner uniquement si le joueur tient l'item team_spawner ou un debug stick. Le bloc utilise RenderType.translucent().")
pdf.p("TeamBlockOverlay affiche une surcouche de debogage (team ID, position, etat du puzzle) quand le joueur vise un bloc de c ur ou de spawn avec un debug stick.")

# ============================================================
# 14. CONFIGURATION
# ============================================================
pdf.add_page()
pdf.ch1("14. Configuration")

pdf.ch2("14.1 Fichier de config")
pdf.p("Le mod utilise le systeme de configuration NeoForge (Config.java). Un fichier de configuration est genere automatiquement dans le repertoire config/ du serveur.")

pdf.ch2("14.2 Options de configuration")
pdf.p("Le CoreDestroyerItem reference une config value COOLDOWN (entier) qui definit le delai entre deux utilisations du Core Destroyer. La classe Config utilise le format NeoForge mod config avec un builder specifie.")

# ============================================================
# FIN
# ============================================================
pdf.ln(10)
pdf.set_x(pdf.l_margin)
pdf.set_font("Helvetica", "I", 8.5)
pdf.set_text_color(120, 120, 120)
pdf.multi_cell(0, 4.5, "Fin du document. Fiche technique du mod XII Days v1.6.2 pour Minecraft 1.21.1 / NeoForge 21.1.219.")

# Save
out = "T:\\XIIDays\\twelves-daysmod-1.21.1\\fiche_technique_xiidays_v1.6.2.pdf"
pdf.output(out)
print(f"PDF genere : {out}")
