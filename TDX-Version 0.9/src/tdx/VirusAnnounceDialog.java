package tdx;

import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * VirusAnnounceDialog — Anuncio de virus estilo "Dead Ahead: Zombie Warfare".
 *
 * Características: • Personaje "Dr. Byte" con animación de 3 estados (idle /
 * habla / señala). • Cuadro de diálogo con texto que aparece letra a letra
 * (typewriter). • Panel lateral con: forma del virus, datos clínicos,
 * tratamiento. • Navegación entre múltiples virus con botón "SIGUIENTE →". •
 * Fondo oscuro con overlay semitransparente (simula pausa de juego).
 *
 * ══ INTEGRACIÓN ═══════════════════════════════════════════════════════ Llama
 * en tu bucle de juego cuando wave == adminCfg.virusAnnounceWave:
 *
 * VirusAnnounceDialog dlg = new VirusAnnounceDialog(gameFrame);
 * dlg.showAnnounce(); // bloquea hasta que el jugador cierra
 */
public class VirusAnnounceDialog extends JDialog {

    // ── Paleta ────────────────────────────────────────────────────────────
    private static final Color BG_OVERLAY = new Color(4, 8, 18, 230);
    private static final Color BG_PANEL = new Color(12, 18, 35);
    private static final Color BG_CARD = new Color(20, 30, 55);
    private static final Color BG_VIRUS_BOX = new Color(10, 22, 40);
    private static final Color ACCENT_CYAN = new Color(0, 200, 220);
    private static final Color ACCENT_RED = new Color(220, 50, 60);
    private static final Color ACCENT_YELLOW = new Color(255, 200, 40);
    private static final Color ACCENT_GREEN = new Color(50, 210, 120);
    private static final Color ACCENT_PURPLE = new Color(160, 80, 240);
    private static final Color TEXT_PRIMARY = new Color(220, 235, 255);
    private static final Color TEXT_MUTED = new Color(90, 115, 160);
    private static final Color BORDER_CYAN = new Color(0, 200, 220, 70);

    // ── Datos de virus ────────────────────────────────────────────────────
    private static final List<VirusData> VIRUS_LIST = buildVirusList();

    // ── Estado animación ──────────────────────────────────────────────────
    /**
     * 0 = idle, 1 = hablando, 2 = señalando
     */
    private int animState = 0;
    private long animTimer = 0;
    private int blinkCounter = 0;

    // ── Estado UI ─────────────────────────────────────────────────────────
    private int currentVirusIndex = 0;
    private int typewriterPos = 0;
    private String fullText = "";
    private boolean typing = false;

    // ── Componentes ───────────────────────────────────────────────────────
    private CharacterPanel characterPanel;
    private JTextArea dialogText;
    private VirusInfoPanel virusInfoPanel;
    private JButton btnNext;
    private JLabel nameLabel;
    private JLabel waveLabel;
    private Timer gameLoopTimer;
    private Timer typewriterTimer;

    // ─────────────────────────────────────────────────────────────────────
    public VirusAnnounceDialog(Frame owner) {
        super(owner, "Alerta de Virus", true);
        setUndecorated(true);
        buildUI();
        pack();
        setLocationRelativeTo(owner);
    }

    VirusAnnounceDialog() {
        throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
    }

    // ─────────────────────────────────────────────────────────────────────
    public void showAnnounce() {
        loadVirus(0);
        startAnimLoop();
        setVisible(true);
    }

    // ── Detener timers al cerrar ──────────────────────────────────────────
    @Override
    public void dispose() {
        if (gameLoopTimer != null) {
            gameLoopTimer.stop();
        }
        if (typewriterTimer != null) {
            typewriterTimer.stop();
        }
        super.dispose();
    }

    // ─────────────────────────────────────────────────────────────────────
    // BUILD UI
    // ─────────────────────────────────────────────────────────────────────
    private void buildUI() {
        // ── Raíz ─────────────────────────────────────────────────────────
        JPanel root = new JPanel(new BorderLayout(0, 0)) {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g;
                g2.setColor(BG_OVERLAY);
                g2.fillRect(0, 0, getWidth(), getHeight());
            }
        };
        root.setBorder(BorderFactory.createLineBorder(BORDER_CYAN, 2));
        root.setPreferredSize(new Dimension(820, 520));
        root.setOpaque(false);

        // ── Título superior ───────────────────────────────────────────────
        root.add(buildTopBar(), BorderLayout.NORTH);

        // ── Contenido central ─────────────────────────────────────────────
        JPanel center = new JPanel(new BorderLayout(0, 0));
        center.setOpaque(false);

        // Personaje izquierda
        characterPanel = new CharacterPanel();
        characterPanel.setPreferredSize(new Dimension(200, 340));
        center.add(characterPanel, BorderLayout.WEST);

        // Info del virus derecha
        virusInfoPanel = new VirusInfoPanel();
        center.add(virusInfoPanel, BorderLayout.CENTER);

        root.add(center, BorderLayout.CENTER);

        // ── Caja de diálogo abajo ─────────────────────────────────────────
        root.add(buildDialogBox(), BorderLayout.SOUTH);

        setContentPane(root);
    }

    // ── Barra superior ────────────────────────────────────────────────────
    private JPanel buildTopBar() {
        JPanel p = new JPanel(new BorderLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g;
                g2.setColor(new Color(8, 14, 30));
                g2.fillRect(0, 0, getWidth(), getHeight());
                g2.setColor(ACCENT_RED);
                g2.setStroke(new BasicStroke(2f));
                g2.drawLine(0, getHeight() - 1, getWidth(), getHeight() - 1);
            }
        };
        p.setOpaque(false);
        p.setPreferredSize(new Dimension(820, 52));
        p.setBorder(new EmptyBorder(8, 16, 8, 16));

        // Ícono + título
        JLabel ico = makeLabel("🦠", 22, ACCENT_RED);
        JLabel title = makeLabel("  ALERTA EPIDEMIOLÓGICA — INFORME DE CAMPO", 14, ACCENT_RED, Font.BOLD);
        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        left.setOpaque(false);
        left.add(ico);
        left.add(title);

        waveLabel = makeLabel("RONDA  •  —", 11, TEXT_MUTED);
        JButton btnClose = makeCloseBtn();

        p.add(left, BorderLayout.WEST);
        p.add(waveLabel, BorderLayout.CENTER);
        p.add(btnClose, BorderLayout.EAST);
        return p;
    }

    private JButton makeCloseBtn() {
        JButton b = new JButton("✕") {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setColor(getModel().isRollover() ? ACCENT_RED : TEXT_MUTED);
                g2.setFont(getFont());
                FontMetrics fm = g2.getFontMetrics();
                g2.drawString(getText(),
                        (getWidth() - fm.stringWidth(getText())) / 2,
                        (getHeight() + fm.getAscent() - fm.getDescent()) / 2);
                g2.dispose();
            }
        };
        b.setFont(new Font("SansSerif", Font.BOLD, 14));
        b.setPreferredSize(new Dimension(30, 30));
        b.setContentAreaFilled(false);
        b.setBorderPainted(false);
        b.setFocusPainted(false);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.addActionListener(e -> dispose());
        return b;
    }

    // ── Caja de diálogo ───────────────────────────────────────────────────
    private JPanel buildDialogBox() {
        JPanel p = new JPanel(new BorderLayout(12, 0)) {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g;
                g2.setColor(new Color(8, 14, 28));
                g2.fillRect(0, 0, getWidth(), getHeight());
                g2.setColor(BORDER_CYAN);
                g2.setStroke(new BasicStroke(1f));
                g2.drawLine(0, 0, getWidth(), 0);
            }
        };
        p.setOpaque(false);
        p.setBorder(new EmptyBorder(12, 16, 12, 16));
        p.setPreferredSize(new Dimension(820, 110));

        // Nombre del personaje
        nameLabel = makeLabel("Dr. Byte", 12, ACCENT_CYAN, Font.BOLD);

        // Texto con efecto typewriter
        dialogText = new JTextArea();
        dialogText.setFont(new Font("Monospaced", Font.PLAIN, 13));
        dialogText.setForeground(TEXT_PRIMARY);
        dialogText.setBackground(new Color(0, 0, 0, 0));
        dialogText.setOpaque(false);
        dialogText.setEditable(false);
        dialogText.setLineWrap(true);
        dialogText.setWrapStyleWord(true);
        dialogText.setBorder(null);

        JScrollPane scroll = new JScrollPane(dialogText);
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        scroll.setBorder(null);
        scroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_NEVER);

        // Botón siguiente
        btnNext = buildNextButton();

        JPanel left = new JPanel(new BorderLayout(0, 4));
        left.setOpaque(false);
        left.add(nameLabel, BorderLayout.NORTH);
        left.add(scroll, BorderLayout.CENTER);

        p.add(left, BorderLayout.CENTER);
        p.add(btnNext, BorderLayout.EAST);
        return p;
    }

    private JButton buildNextButton() {
        JButton b = new JButton("SIGUIENTE  →") {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                Color bg = getModel().isRollover() ? new Color(0, 200, 220, 60) : new Color(0, 0, 0, 0);
                g2.setColor(bg);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                g2.setColor(ACCENT_CYAN);
                g2.setStroke(new BasicStroke(1.5f));
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 8, 8);
                FontMetrics fm = g2.getFontMetrics();
                g2.drawString(getText(),
                        (getWidth() - fm.stringWidth(getText())) / 2,
                        (getHeight() + fm.getAscent() - fm.getDescent()) / 2);
                g2.dispose();
            }
        };
        b.setFont(new Font("SansSerif", Font.BOLD, 13));
        b.setForeground(ACCENT_CYAN);
        b.setPreferredSize(new Dimension(160, 70));
        b.setContentAreaFilled(false);
        b.setBorderPainted(false);
        b.setFocusPainted(false);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.addActionListener(e -> onNext());
        return b;
    }

    // ─────────────────────────────────────────────────────────────────────
    // LÓGICA
    // ─────────────────────────────────────────────────────────────────────
    private void loadVirus(int idx) {
        currentVirusIndex = idx;
        VirusData v = VIRUS_LIST.get(idx);
        virusInfoPanel.setVirus(v);

        // Decidir qué texto dice el Dr. Byte
        String intro = idx == 0
                ? "¡Atención, comandante! He detectado una nueva amenaza biológica en el campo de batalla. Presta atención a este informe."
                : "Continuando el informe... Aquí tienes información sobre el siguiente agente patógeno identificado.";
        startTypewriter(intro);

        // Cambiar botón si es el último
        btnNext.setText(idx >= VIRUS_LIST.size() - 1 ? "ENTENDIDO  ✓" : "SIGUIENTE  →");
        waveLabel.setText("INFORME  " + (idx + 1) + " / " + VIRUS_LIST.size());

        // El personaje señala durante unos segundos
        animState = 2;
    }

    private void onNext() {
        if (typing) {
            // Mostrar texto completo de golpe
            showFullText();
            return;
        }
        int next = currentVirusIndex + 1;
        if (next < VIRUS_LIST.size()) {
            loadVirus(next);
        } else {
            dispose();
        }
    }

    // ── Typewriter ────────────────────────────────────────────────────────
    private void startTypewriter(String text) {
        fullText = text;
        typewriterPos = 0;
        typing = true;
        dialogText.setText("");
        animState = 1; // estado "hablando"

        if (typewriterTimer != null) {
            typewriterTimer.stop();
        }
        typewriterTimer = new Timer(32, e -> {
            if (typewriterPos < fullText.length()) {
                typewriterPos++;
                dialogText.setText(fullText.substring(0, typewriterPos));
                // pequeño parpadeo del cursor
                if (typewriterPos % 3 == 0) {
                    characterPanel.repaint();
                }
            } else {
                ((Timer) e.getSource()).stop();
                typing = false;
                animState = 0; // vuelve a idle
            }
        });
        typewriterTimer.start();
    }

    private void showFullText() {
        if (typewriterTimer != null) {
            typewriterTimer.stop();
        }
        typewriterPos = fullText.length();
        dialogText.setText(fullText);
        typing = false;
        animState = 0;
    }

    // ── Bucle de animación ────────────────────────────────────────────────
    private void startAnimLoop() {
        gameLoopTimer = new Timer(80, e -> {
            animTimer++;
            // parpadeo idle
            blinkCounter++;
            if (blinkCounter > 18) {
                blinkCounter = 0;
            }
            characterPanel.repaint();
        });
        gameLoopTimer.start();
    }

    // ─────────────────────────────────────────────────────────────────────
    // PANEL DEL PERSONAJE  —  Dr. Byte
    // Dibujado en Java2D puro: sin imágenes externas.
    // 3 estados: 0=idle, 1=hablando, 2=señalando
    // ─────────────────────────────────────────────────────────────────────
    private class CharacterPanel extends JPanel {

        CharacterPanel() {
            setOpaque(false);
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int cx = getWidth() / 2;
            int cy = getHeight() / 2 - 20;

            drawBackground(g2, cx, cy);
            drawBody(g2, cx, cy);
            drawHead(g2, cx, cy);
            drawArm(g2, cx, cy, animState);
            drawFace(g2, cx, cy, animState);
            drawLabCoat(g2, cx, cy);
            drawNameTag(g2, cx, cy);
        }

        // Halo de fondo
        private void drawBackground(Graphics2D g2, int cx, int cy) {
            RadialGradientPaint rg = new RadialGradientPaint(
                    cx, cy + 50, 90,
                    new float[]{0f, 1f},
                    new Color[]{new Color(0, 180, 220, 30), new Color(0, 0, 0, 0)}
            );
            g2.setPaint(rg);
            g2.fillOval(cx - 90, cy - 40, 180, 180);
        }

        // Cuerpo (bata de laboratorio)
        private void drawBody(Graphics2D g2, int cx, int cy) {
            // Torso base
            g2.setColor(new Color(200, 210, 230));
            g2.fillRoundRect(cx - 28, cy + 38, 56, 80, 10, 10);
            // Bata — solapas
            g2.setColor(new Color(240, 245, 255));
            int[] xL = {cx - 28, cx - 10, cx};
            int[] yL = {cy + 38, cy + 38, cy + 70};
            g2.fillPolygon(xL, yL, 3);
            int[] xR = {cx + 28, cx + 10, cx};
            g2.fillPolygon(xR, yL, 3);
        }

        // Bata — detalles
        private void drawLabCoat(Graphics2D g2, int cx, int cy) {
            // Bolsillo
            g2.setColor(new Color(180, 190, 210));
            g2.fillRoundRect(cx + 6, cy + 70, 16, 20, 4, 4);
            g2.setColor(new Color(0, 180, 220, 120));
            g2.setStroke(new BasicStroke(1f));
            g2.drawRoundRect(cx + 6, cy + 70, 16, 20, 4, 4);
            // Bolígrafo
            g2.setColor(ACCENT_CYAN);
            g2.setStroke(new BasicStroke(2f));
            g2.drawLine(cx + 12, cy + 71, cx + 12, cy + 80);
            g2.drawLine(cx + 15, cy + 71, cx + 15, cy + 80);
        }

        // Cabeza
        private void drawHead(Graphics2D g2, int cx, int cy) {
            // Cuello
            g2.setColor(new Color(255, 210, 170));
            g2.fillRoundRect(cx - 8, cy + 24, 16, 18, 4, 4);
            // Cabeza
            g2.setColor(new Color(255, 220, 180));
            g2.fillOval(cx - 26, cy - 26, 52, 52);
            // Contorno
            g2.setColor(new Color(200, 160, 120));
            g2.setStroke(new BasicStroke(1.5f));
            g2.drawOval(cx - 26, cy - 26, 52, 52);
            // Cabello
            g2.setColor(new Color(60, 40, 20));
            g2.fillArc(cx - 26, cy - 30, 52, 30, 10, 160);
        }

        // Brazo animado
        private void drawArm(Graphics2D g2, int cx, int cy, int state) {
            // Brazo izquierdo (quieto, doblado)
            g2.setColor(new Color(200, 210, 230));
            g2.setStroke(new BasicStroke(10f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2.drawLine(cx - 28, cy + 50, cx - 50, cy + 75);
            g2.setColor(new Color(255, 210, 170));
            g2.drawLine(cx - 50, cy + 75, cx - 48, cy + 95);

            // Brazo derecho — animado según estado
            double armAngle;
            boolean showPointer = false;
            if (state == 0) {
                // idle: brazo oscilando suavemente arriba/abajo
                armAngle = Math.toRadians(-20 + 10 * Math.sin(animTimer * 0.15));
            } else if (state == 1) {
                // hablando: brazo oscila más rápido
                armAngle = Math.toRadians(-30 + 20 * Math.sin(animTimer * 0.4));
            } else {
                // señalando: brazo extendido hacia el panel
                armAngle = Math.toRadians(25);
                showPointer = true;
            }

            // Segmento superior del brazo
            int ax1 = (int) (cx + 28 + 32 * Math.cos(armAngle - Math.PI / 2));
            int ay1 = (int) (cy + 50 + 32 * Math.sin(armAngle - Math.PI / 2));
            g2.setColor(new Color(200, 210, 230));
            g2.setStroke(new BasicStroke(10f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2.drawLine(cx + 28, cy + 50, ax1, ay1);

            // Antebrazo
            double foreAngle = armAngle + (state == 2 ? Math.toRadians(0) : Math.toRadians(-30));
            int ax2 = (int) (ax1 + 28 * Math.cos(foreAngle - Math.PI / 2));
            int ay2 = (int) (ay1 + 28 * Math.sin(foreAngle - Math.PI / 2));
            g2.setColor(new Color(255, 210, 170));
            g2.setStroke(new BasicStroke(8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2.drawLine(ax1, ay1, ax2, ay2);

            // Puntero (cuando señala)
            if (showPointer) {
                g2.setColor(ACCENT_YELLOW);
                g2.setStroke(new BasicStroke(2f));
                g2.drawLine(ax2, ay2, ax2 + 20, ay2 - 5);
                g2.fillOval(ax2 + 16, ay2 - 9, 8, 8);
            }
        }

        // Cara (ojos, boca animados)
        private void drawFace(Graphics2D g2, int cx, int cy, int state) {
            boolean blink = blinkCounter > 16;

            // Gafas
            g2.setColor(new Color(60, 60, 80));
            g2.setStroke(new BasicStroke(1.5f));
            g2.drawRoundRect(cx - 22, cy - 8, 16, 12, 6, 6);
            g2.drawRoundRect(cx + 6, cy - 8, 16, 12, 6, 6);
            g2.drawLine(cx - 6, cy - 3, cx + 6, cy - 3);  // puente
            // cristales
            g2.setColor(new Color(100, 200, 240, 80));
            g2.fillRoundRect(cx - 22, cy - 8, 16, 12, 6, 6);
            g2.fillRoundRect(cx + 6, cy - 8, 16, 12, 6, 6);

            // Ojos (pupila o línea si parpadea)
            g2.setColor(new Color(30, 30, 50));
            if (blink) {
                g2.setStroke(new BasicStroke(2f));
                g2.drawLine(cx - 19, cy - 2, cx - 10, cy - 2);
                g2.drawLine(cx + 9, cy - 2, cx + 18, cy - 2);
            } else {
                g2.fillOval(cx - 17, cy - 6, 8, 8);
                g2.fillOval(cx + 10, cy - 6, 8, 8);
                // brillo
                g2.setColor(Color.WHITE);
                g2.fillOval(cx - 15, cy - 5, 3, 3);
                g2.fillOval(cx + 12, cy - 5, 3, 3);
            }

            // Nariz
            g2.setColor(new Color(210, 165, 130));
            g2.setStroke(new BasicStroke(1.5f));
            g2.drawArc(cx - 4, cy + 2, 8, 6, 210, 120);

            // Boca según estado
            g2.setColor(new Color(160, 90, 80));
            g2.setStroke(new BasicStroke(2f));
            if (state == 0) {
                // idle: sonrisa leve
                g2.drawArc(cx - 10, cy + 10, 20, 10, 200, 140);
            } else if (state == 1) {
                // hablando: boca alterna abierta/cerrada
                boolean mouthOpen = (animTimer % 6) < 3;
                if (mouthOpen) {
                    g2.setColor(new Color(80, 30, 30));
                    g2.fillOval(cx - 8, cy + 11, 16, 8);
                    g2.setColor(new Color(255, 150, 150));
                    g2.fillOval(cx - 5, cy + 14, 10, 5); // lengua
                } else {
                    g2.drawLine(cx - 8, cy + 15, cx + 8, cy + 15);
                }
            } else {
                // señalando: boca en "O" de sorpresa
                g2.setColor(new Color(80, 30, 30));
                g2.fillOval(cx - 6, cy + 10, 12, 10);
            }
        }

        // Nombre del personaje
        private void drawNameTag(Graphics2D g2, int cx, int cy) {
            String name = "Dr. Byte";
            g2.setFont(new Font("SansSerif", Font.BOLD, 11));
            FontMetrics fm = g2.getFontMetrics();
            int tw = fm.stringWidth(name) + 12;
            int tx = cx - tw / 2;
            int ty = cy + 130;
            g2.setColor(new Color(0, 180, 220, 140));
            g2.fillRoundRect(tx, ty, tw, 18, 6, 6);
            g2.setColor(ACCENT_CYAN);
            g2.setStroke(new BasicStroke(1f));
            g2.drawRoundRect(tx, ty, tw, 18, 6, 6);
            g2.setColor(Color.WHITE);
            g2.drawString(name, tx + 6, ty + 13);
        }
    }

    // ─────────────────────────────────────────────────────────────────────
    // PANEL INFO DEL VIRUS
    // ─────────────────────────────────────────────────────────────────────
    private class VirusInfoPanel extends JPanel {

        private VirusData virus;

        VirusInfoPanel() {
            setLayout(new BorderLayout(0, 0));
            setOpaque(false);
        }

        void setVirus(VirusData v) {
            this.virus = v;
            removeAll();
            buildContent();
            revalidate();
            repaint();
        }

        private void buildContent() {
            if (virus == null) {
                return;
            }

            JPanel wrapper = new JPanel(new BorderLayout(10, 0)) {
                @Override
                protected void paintComponent(Graphics g) {
                    super.paintComponent(g);
                    Graphics2D g2 = (Graphics2D) g;
                    g2.setColor(BG_CARD);
                    g2.fillRect(0, 0, getWidth(), getHeight());
                }
            };
            wrapper.setOpaque(false);
            wrapper.setBorder(new EmptyBorder(12, 8, 8, 14));

            // ── Forma del virus (dibujo vectorial) ────────────────────────
            VirusShapePanel shapePanel = new VirusShapePanel(virus);
            shapePanel.setPreferredSize(new Dimension(180, 220));
            wrapper.add(shapePanel, BorderLayout.WEST);

            // ── Datos clínicos + tratamiento ──────────────────────────────
            JPanel data = new JPanel();
            data.setLayout(new BoxLayout(data, BoxLayout.Y_AXIS));
            data.setOpaque(false);

            // Nombre del virus
            data.add(makeLabel("⬡  " + virus.name.toUpperCase(), 16, virus.color, Font.BOLD));
            data.add(Box.createVerticalStrut(4));
            data.add(makeLabel(virus.scientificName, 11, TEXT_MUTED, Font.ITALIC));
            data.add(Box.createVerticalStrut(10));

            // Separador
            data.add(makeSeparator());
            data.add(Box.createVerticalStrut(8));

            // Clasificación
            data.add(makeInfoRow("🔬 Tipo", virus.type, ACCENT_CYAN));
            data.add(Box.createVerticalStrut(4));
            data.add(makeInfoRow("⚡ Peligrosidad", virus.danger + "/5", virus.dangerColor()));
            data.add(Box.createVerticalStrut(4));
            data.add(makeInfoRow("🎯 Objetivo", virus.target, TEXT_PRIMARY));
            data.add(Box.createVerticalStrut(8));

            // Síntomas
            data.add(makeLabel("📋 SÍNTOMAS", 11, ACCENT_YELLOW, Font.BOLD));
            data.add(Box.createVerticalStrut(4));
            for (String s : virus.symptoms) {
                JLabel l = makeLabel("  • " + s, 11, TEXT_PRIMARY);
                l.setAlignmentX(Component.LEFT_ALIGNMENT);
                data.add(l);
                data.add(Box.createVerticalStrut(2));
            }
            data.add(Box.createVerticalStrut(8));

            // Tratamiento
            data.add(makeLabel("💊 TRATAMIENTO", 11, ACCENT_GREEN, Font.BOLD));
            data.add(Box.createVerticalStrut(4));
            JLabel treatLabel = makeLabel("<html><body style='width:280px'>" + virus.treatment + "</body></html>",
                    11, TEXT_PRIMARY);
            treatLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
            data.add(treatLabel);

            wrapper.add(data, BorderLayout.CENTER);
            add(wrapper, BorderLayout.CENTER);
        }

        private JPanel makeSeparator() {
            JPanel sep = new JPanel() {
                @Override
                protected void paintComponent(Graphics g) {
                    super.paintComponent(g);
                    Graphics2D g2 = (Graphics2D) g;
                    g2.setColor(BORDER_CYAN);
                    g2.drawLine(0, 0, getWidth(), 0);
                }
            };
            sep.setOpaque(false);
            sep.setMaximumSize(new Dimension(600, 2));
            sep.setPreferredSize(new Dimension(600, 2));
            return sep;
        }

        private JPanel makeInfoRow(String label, String value, Color valueColor) {
            JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
            row.setOpaque(false);
            row.setAlignmentX(Component.LEFT_ALIGNMENT);
            row.add(makeLabel(label + ":", 11, TEXT_MUTED, Font.BOLD));
            row.add(makeLabel(value, 11, valueColor, Font.PLAIN));
            return row;
        }
    }

    // ─────────────────────────────────────────────────────────────────────
    // PANEL FORMA DEL VIRUS (dibujado en Java2D)
    // ─────────────────────────────────────────────────────────────────────
    private class VirusShapePanel extends JPanel {

        private final VirusData v;
        private float pulseT = 0;
        private Timer pulseTimer;

        VirusShapePanel(VirusData v) {
            this.v = v;
            setOpaque(false);
            // Pulso suave
            pulseTimer = new Timer(50, e -> {
                pulseT += 0.08f;
                repaint();
            });
            pulseTimer.start();
            addHierarchyListener(e -> {
                if (!isDisplayable() && pulseTimer != null) {
                    pulseTimer.stop();
                }
            });
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int cx = getWidth() / 2;
            int cy = getHeight() / 2 - 10;

            // Fondo de la caja del virus
            g2.setColor(BG_VIRUS_BOX);
            g2.fillRoundRect(4, 4, getWidth() - 8, getHeight() - 8, 12, 12);
            g2.setColor(v.color.darker());
            g2.setStroke(new BasicStroke(1f));
            g2.drawRoundRect(4, 4, getWidth() - 8, getHeight() - 8, 12, 12);

            float pulse = (float) (0.92 + 0.08 * Math.sin(pulseT));

            switch (v.shape) {
                case SPHERE:
                    drawSphereVirus(g2, cx, cy, pulse);
                    break;
                case ICOSAHEDRON:
                    drawIcosaVirus(g2, cx, cy, pulse);
                    break;
                case ELONGATED:
                    drawElongatedVirus(g2, cx, cy, pulse);
                    break;
                case BACTERIUM:
                    drawBacteriumVirus(g2, cx, cy, pulse);
                    break;
                case CORONA:
                    drawCoronaVirus(g2, cx, cy, pulse);
                    break;
            }

            // Etiqueta debajo
            g2.setFont(new Font("Monospaced", Font.BOLD, 10));
            g2.setColor(v.color);
            String tag = "[" + v.type + "]";
            FontMetrics fm = g2.getFontMetrics();
            g2.drawString(tag, cx - fm.stringWidth(tag) / 2, getHeight() - 10);
        }

        private void drawSphereVirus(Graphics2D g2, int cx, int cy, float pulse) {
            int r = (int) (38 * pulse);
            // Glow
            RadialGradientPaint rg = new RadialGradientPaint(cx, cy, r + 20,
                    new float[]{0f, 0.6f, 1f},
                    new Color[]{new Color(v.color.getRed(), v.color.getGreen(), v.color.getBlue(), 80),
                        new Color(v.color.getRed(), v.color.getGreen(), v.color.getBlue(), 20),
                        new Color(0, 0, 0, 0)});
            g2.setPaint(rg);
            g2.fillOval(cx - r - 20, cy - r - 20, (r + 20) * 2, (r + 20) * 2);
            // Esfera
            RadialGradientPaint body = new RadialGradientPaint(cx - r / 3, cy - r / 3, r,
                    new float[]{0f, 1f},
                    new Color[]{v.color.brighter(), v.color.darker()});
            g2.setPaint(body);
            g2.fillOval(cx - r, cy - r, r * 2, r * 2);
            // Espículas (proyecciones)
            g2.setColor(v.color.brighter());
            g2.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            for (int i = 0; i < 12; i++) {
                double a = Math.toRadians(i * 30 + pulseT * 5);
                int x1 = (int) (cx + r * Math.cos(a));
                int y1 = (int) (cy + r * Math.sin(a));
                int x2 = (int) (cx + (r + 12) * Math.cos(a));
                int y2 = (int) (cy + (r + 12) * Math.sin(a));
                g2.drawLine(x1, y1, x2, y2);
                g2.fillOval(x2 - 3, y2 - 3, 6, 6);
            }
        }

        private void drawIcosaVirus(Graphics2D g2, int cx, int cy, float pulse) {
            int r = (int) (36 * pulse);
            // Polígono 20 caras simplificado como hexágono irregular
            g2.setColor(new Color(v.color.getRed(), v.color.getGreen(), v.color.getBlue(), 60));
            g2.fillOval(cx - r - 15, cy - r - 15, (r + 15) * 2, (r + 15) * 2);

            Polygon hex = new Polygon();
            for (int i = 0; i < 6; i++) {
                double a = Math.toRadians(i * 60 - 30);
                hex.addPoint((int) (cx + r * Math.cos(a)), (int) (cy + r * Math.sin(a)));
            }
            GradientPaint gp = new GradientPaint(cx - r, cy - r, v.color.brighter(), cx + r, cy + r, v.color.darker());
            g2.setPaint(gp);
            g2.fill(hex);
            g2.setColor(v.color.brighter().brighter());
            g2.setStroke(new BasicStroke(1.5f));
            g2.draw(hex);
            // Líneas internas tipo ADN
            g2.setColor(new Color(255, 255, 255, 60));
            for (int i = 0; i < 6; i++) {
                double a = Math.toRadians(i * 60 - 30);
                g2.drawLine(cx, cy, (int) (cx + r * Math.cos(a)), (int) (cy + r * Math.sin(a)));
            }
        }

        private void drawElongatedVirus(Graphics2D g2, int cx, int cy, float pulse) {
            int rw = (int) (50 * pulse), rh = (int) (28 * pulse);
            // Glow
            g2.setColor(new Color(v.color.getRed(), v.color.getGreen(), v.color.getBlue(), 40));
            g2.fillOval(cx - rw - 10, cy - rh - 10, (rw + 10) * 2, (rh + 10) * 2);
            // Cuerpo ovalado
            GradientPaint gp = new GradientPaint(cx, cy - rh, v.color.brighter(), cx, cy + rh, v.color.darker());
            g2.setPaint(gp);
            g2.fillOval(cx - rw, cy - rh, rw * 2, rh * 2);
            // Espinas
            g2.setColor(v.color.brighter());
            g2.setStroke(new BasicStroke(1.5f));
            for (int i = 0; i < 16; i++) {
                double a = Math.toRadians(i * 22.5);
                double rx = rw * Math.cos(a), ry = rh * Math.sin(a);
                double len = 1 + 10 / Math.sqrt(rx * rx + ry * ry);
                g2.drawLine((int) (cx + rx), (int) (cy + ry),
                        (int) (cx + rx * (1 + len * 0.25)), (int) (cy + ry * (1 + len * 0.25)));
            }
        }

        private void drawBacteriumVirus(Graphics2D g2, int cx, int cy, float pulse) {
            int r = (int) (32 * pulse);
            // Bacteria con flagelo
            GradientPaint gp = new GradientPaint(cx - r, cy, v.color.brighter(), cx + r, cy + r, v.color.darker());
            g2.setPaint(gp);
            g2.fillRoundRect(cx - r, cy - r / 2, r * 2, r + r / 2, 20, 20);
            // Cápsula
            g2.setColor(new Color(v.color.getRed(), v.color.getGreen(), v.color.getBlue(), 100));
            g2.setStroke(new BasicStroke(3f));
            g2.drawRoundRect(cx - r - 4, cy - r / 2 - 4, r * 2 + 8, r + r / 2 + 8, 24, 24);
            // Flagelo
            g2.setColor(v.color.brighter());
            g2.setStroke(new BasicStroke(2f));
            Path2D flagelo = new Path2D.Double();
            flagelo.moveTo(cx + r, cy);
            for (int i = 0; i < 40; i++) {
                double t = i / 40.0;
                double fx = cx + r + t * 40;
                double fy = cy + 12 * Math.sin(t * 3 * Math.PI + pulseT);
                if (i == 0) {
                    flagelo.moveTo(fx, fy);
                } else {
                    flagelo.lineTo(fx, fy);
                }
            }
            g2.draw(flagelo);
            // Núcleo
            g2.setColor(new Color(0, 0, 0, 80));
            g2.fillOval(cx - 10, cy - 10, 20, 20);
        }

        private void drawCoronaVirus(Graphics2D g2, int cx, int cy, float pulse) {
            int r = (int) (32 * pulse);
            // Glow
            RadialGradientPaint rg = new RadialGradientPaint(cx, cy, r + 30,
                    new float[]{0f, 1f},
                    new Color[]{new Color(v.color.getRed(), v.color.getGreen(), v.color.getBlue(), 60),
                        new Color(0, 0, 0, 0)});
            g2.setPaint(rg);
            g2.fillOval(cx - r - 30, cy - r - 30, (r + 30) * 2, (r + 30) * 2);
            // Membrana exterior
            g2.setColor(new Color(v.color.getRed(), v.color.getGreen(), v.color.getBlue(), 80));
            g2.setStroke(new BasicStroke(3f));
            g2.drawOval(cx - r - 14, cy - r - 14, (r + 14) * 2, (r + 14) * 2);
            // Esfera central
            RadialGradientPaint body = new RadialGradientPaint(cx - r / 3, cy - r / 3, r,
                    new float[]{0f, 1f},
                    new Color[]{v.color, v.color.darker().darker()});
            g2.setPaint(body);
            g2.fillOval(cx - r, cy - r, r * 2, r * 2);
            // Coronas (spikes grandes)
            g2.setColor(v.color.brighter());
            g2.setStroke(new BasicStroke(3f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            for (int i = 0; i < 10; i++) {
                double a = Math.toRadians(i * 36 + pulseT * 3);
                int x1 = (int) (cx + r * Math.cos(a));
                int y1 = (int) (cy + r * Math.sin(a));
                int x2 = (int) (cx + (r + 18) * Math.cos(a));
                int y2 = (int) (cy + (r + 18) * Math.sin(a));
                g2.drawLine(x1, y1, x2, y2);
                g2.fillOval(x2 - 5, y2 - 5, 10, 10);
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────────
    // DATOS DE VIRUS
    // ─────────────────────────────────────────────────────────────────────
    enum VirusShape {
        SPHERE, ICOSAHEDRON, ELONGATED, BACTERIUM, CORONA
    }

    static class VirusData {

        String name, scientificName, type, target, treatment;
        String[] symptoms;
        int danger;       // 1–5
        Color color;
        VirusShape shape;

        VirusData(String name, String sci, String type, VirusShape shape, Color color,
                int danger, String target, String[] symptoms, String treatment) {
            this.name = name;
            this.scientificName = sci;
            this.type = type;
            this.shape = shape;
            this.color = color;
            this.danger = danger;
            this.target = target;
            this.symptoms = symptoms;
            this.treatment = treatment;
        }

        Color dangerColor() {
            if (danger >= 5) {
                return new Color(220, 40, 40);
            }
            if (danger >= 4) {
                return new Color(220, 120, 40);
            }
            if (danger >= 3) {
                return new Color(220, 200, 40);
            }
            if (danger >= 2) {
                return new Color(120, 200, 40);
            }
            return new Color(40, 200, 120);
        }
    }
// AQUI VAN LOS DATOS DE LOS VIRUS; SI SE AGREGAN SOLO ES DE COIAR Y PEGAR; SE SOBRESCRIBE LOS DATOS Y SE ESCOJE EL TIPO DE VIRUS
// EN EL APARTADO DE VIRUSSHAPE; TAMBIEN EL COLOR SE CAMBIA EN COLOR XD
/*
    switch (v.shape) {
                case SPHERE:
                    drawSphereVirus(g2, cx, cy, pulse);
                    break;
                case ICOSAHEDRON:
                    drawIcosaVirus(g2, cx, cy, pulse);
                    break;
                case ELONGATED:
                    drawElongatedVirus(g2, cx, cy, pulse);
                    break;
                case BACTERIUM:
                    drawBacteriumVirus(g2, cx, cy, pulse);
                    break;
                case CORONA:
                    drawCoronaVirus(g2, cx, cy, pulse);
                    break;
            }
    */
    private static List<VirusData> buildVirusList() {
        List<VirusData> list = new ArrayList<>();
        // 1. COVID-19
        list.add(new VirusData(
                "COVID-19",
                "SARS-CoV-2", 
                "ARN",
                VirusShape.SPHERE,
                new Color(120, 220, 155),
                3,
                "Sistema respiratorio",
                new String[]{"Estornudos, secreción nasal, tos, fiebre malestar general, diarrea y dolor de cabeza."},
                "Guardar reposo, alimentación ligera y de fácil digestión, y abundante líquido. En casos graves, oxigenoterapia y medicamentos específicos." 
        ));

// 2. Gripe
        list.add(new VirusData(
                "Gripe", 
                "Virus de la influenza", 
                "ARN",
                VirusShape.ICOSAHEDRON,
                new Color(250, 250, 250),
                3,
                "Sistema respiratorio",
                new String[]{"Fiebre, escalofríos, secreción nasal, estornudos, dolor de garganta, dolor muscular, cansancio excesivo y pérdida de apetito."}, 
                "Permanecer en reposo, beber mucha agua y tener una alimentación ligera, aparte de medicamentos antiinflamatorios o analgésicos." 
        ));

// 3. Resfriado
        list.add(new VirusData(
                "Resfriado", 
                "Rinovirus", 
                "ARN",
                VirusShape.ELONGATED,
                new Color(80, 80, 80),
                3,
                "Sistema respiratorio",
                new String[]{"Secreción nasal, estornudos frecuentes, congestión nasal, dolor y picazón en la garganta, tos y náuseas."},
                "Descansar, beber muchos líquidos, una dieta ligera y uso de medicamentos antiinflamatorios y analgésicos."
        ));

// 4. Sarampión
        list.add(new VirusData(
                "Sarampión",
                "Familia Paramyxoviridae",
                "ARN",
                VirusShape.BACTERIUM,
                new Color(80, 160, 255),
                3,
                "Sistema respiratorio",
                new String[]{"Manchas rojas en la piel, fiebre alta, tos con flemas, aumento de la sensibilidad a la luz, secreción nasal, pérdida de apetito y mancha blanca en el interior de la mejilla."}, 
                "Reposo, la hidratación y el uso de analgésicos. Además, en algunos casos, se puede recomendar el uso de suplementos de vitamina A."
        ));

// 5. Poliomielitis
        list.add(new VirusData(
                "Poliomielitis", 
                "Poliovirus", 
                "ARN",
                VirusShape.CORONA,
                new Color(80, 160, 255),
                3,
                "Sistema respiratorio",
                new String[]{"Dolor de cabeza, fiebre y cansancio excesivo. En algunos casos, parálisis de una o ambas piernas, atrofia muscular, dificultad para hablar y/o tragar y espasmos musculares."}, 
                "No existe tratamiento específico; no obstante, la fisioterapia está indicada para estimular y favorecer el desarrollo de los músculos atrofiados y mejorar la postura." 
        ));

// 6. Hepatitis
        list.add(new VirusData(
                "Hepatitis", 
                "Virus de la hepatitis", 
                "ARN",
                VirusShape.CORONA,
                new Color(80, 160, 255),
                3,
                "Sistema respiratorio",
                new String[]{"Dolor de cabeza, sensación de malestar general, hinchazón abdominal, náuseas, vómitos, piel y ojos amarillentos, heces claras y orina oscura."}, 
                "Reposo, alimentación ligera y se puede recomendar el uso de medicamentos como interferón, lamivudina y adefovir." 
        ));

// 7. Fiebre amarilla
        list.add(new VirusData(
                "Fiebre amarilla",
                "Género Flavivirus",
                "ARN",
                VirusShape.SPHERE,
                new Color(80, 160, 255),
                3,
                "Sistema respiratorio",
                new String[]{"Fuerte dolor de cabeza, fiebre, escalofríos, mayor sensibilidad a la luz, dolor muscular y aumento de los latidos cardíacos. Puede haber color amarillento de piel/ojos, vómitos con sangre y dolor abdominal."}, // [cite: 50, 51]
                "Reposo, ingesta de abundante líquido durante el día, alimentación ligera y medicamentos que ayuden a aliviar los síntomas." 
        ));

// 8. Varicela
        list.add(new VirusData(
                "Varicela",
                "Virus de la varicela-zóster", 
                "ARN",
                VirusShape.ICOSAHEDRON,
                new Color(80, 160, 255),
                3,
                "Sistema respiratorio",
                new String[]{"Aparición de ampollas rojas por todo el cuerpo que contienen líquido y causan mucha comezón, fiebre, cansancio, malestar general y falta de apetito."},
                "Uso de medicamentos antialérgicos para aliviar la comezón y se recomienda evitar el contacto con otras personas." 
        ));

// 9. Viruela del mono
        list.add(new VirusData(
                "Viruela del mono", 
                "Género Orthopoxvirus", 
                "ARN",
                VirusShape.SPHERE,
                new Color(80, 160, 255),
                3,
                "Sistema respiratorio",
                new String[]{"Surgimiento de ampollas y heridas en la piel, escalofríos, dolor muscular, dolor de cabeza y fiebre."}, 
                "Suelen desaparecer en un máximo de 21 días, pero se puede recomendar el uso de medicamentos sintomáticos o el antiviral tecovirimat." 
        ));

// 10. Fiebre oropouche
        list.add(new VirusData(
                "Fiebre oropouche", 
                "Virus Orthobunyavirus", 
                "ARN",
                VirusShape.ICOSAHEDRON,
                new Color(80, 160, 255),
                3,
                "Sistema respiratorio",
                new String[]{"Fiebre repentina alta, escalofríos, dolor de cabeza intenso, dolor muscular y en las articulaciones, náuseas, diarrea, dolor abdominal o sangrados."},
                "Reposo, aumentar la ingesta de líquidos y el uso de analgésicos y antipiréticos, como paracetamol o dipirona." 
        ));
        return list;

    }

    // ── Helpers globales ──────────────────────────────────────────────────
    private static JLabel makeLabel(String text, int size, Color color) {
        return makeLabel(text, size, color, Font.PLAIN);
    }

    private static JLabel makeLabel(String text, int size, Color color, int style) {
        JLabel l = new JLabel(text);
        l.setFont(new Font("SansSerif", style, size));
        l.setForeground(color);
        l.setAlignmentX(Component.LEFT_ALIGNMENT);
        return l;
    }

    // ── Demo ─────────────────────────────────────────────────────────────
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception ignored) {
            }
            VirusAnnounceDialog dlg = new VirusAnnounceDialog(null);
            dlg.showAnnounce();
            System.exit(0);
        });
    }
}
