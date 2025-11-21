package org.gui;

import org.db.DatabaseHelper;
import org.db.UserManager;
import org.models.User;

import javax.swing.*;
import java.awt.*;

public class LoginDialog extends JDialog {
    private User result = null;
    private DatabaseHelper db;
    private UserManager userManager;

    public LoginDialog(Frame owner) {
        super(owner, "Inicio de Sesión - Restaurante", true);
        this.db = new DatabaseHelper();
        this.userManager = new UserManager();
        // Ensure the default demo users and their password hashes exist in the database
        this.userManager.initializeDefaultUsers();

        setLayout(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(8,8,8,8);
        c.fill = GridBagConstraints.HORIZONTAL;

        JTextField txtUser = new JTextField(15);
        JPasswordField txtPassword = new JPasswordField(15);
        JButton btnLogin = new JButton("Iniciar Sesión");
        JButton btnCancel = new JButton("Cancelar");

        // Header
        JLabel titleLabel = new JLabel("Sistema de Gestión de Restaurante", JLabel.CENTER);
        titleLabel.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 16));
        c.gridx=0; c.gridy=0; c.gridwidth=2; add(titleLabel, c);

        // Login form
        c.gridwidth=1;
        c.gridx=0; c.gridy=1; add(new JLabel("Usuario:"), c);
        c.gridx=1; add(txtUser, c);
        c.gridx=0; c.gridy=2; add(new JLabel("Contraseña:"), c);
        c.gridx=1; add(txtPassword, c);

        // Buttons
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttonPanel.add(btnLogin); 
        buttonPanel.add(btnCancel);
        c.gridx=0; c.gridy=3; c.gridwidth=2; add(buttonPanel, c);

        // Info panel
        JPanel infoPanel = new JPanel();
        infoPanel.setLayout(new BoxLayout(infoPanel, BoxLayout.Y_AXIS));
        infoPanel.setBorder(BorderFactory.createTitledBorder("Credenciales de Prueba"));
        infoPanel.add(new JLabel("• admin / admin123 (Administrador)"));
        infoPanel.add(new JLabel("• waiter1 / waiter123 (Mesero)"));
        infoPanel.add(new JLabel("• chef1 / chef123 (Cocinero)"));
        c.gridx=0; c.gridy=4; c.gridwidth=2; add(infoPanel, c);

        btnLogin.addActionListener(e -> {
            String username = txtUser.getText().trim();
            String password = new String(txtPassword.getPassword());
            
            if (username.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Por favor ingrese un nombre de usuario.", "Error de Inicio de Sesión", JOptionPane.ERROR_MESSAGE);
                return;
            }
            
            if (password.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Por favor ingrese una contraseña.", "Error de Inicio de Sesión", JOptionPane.ERROR_MESSAGE);
                return;
            }

            // Authenticate against database
            User user = authenticateUser(username, password);
            if (user != null) {
                result = user;
                setVisible(false);
            } else {
                JOptionPane.showMessageDialog(this, 
                    "Usuario o contraseña incorrectos.\nIntente con las credenciales de prueba mostradas abajo.", 
                    "Inicio de Sesión Fallido", 
                    JOptionPane.ERROR_MESSAGE);
                txtPassword.setText("");
                txtUser.requestFocus();
            }
        });

        btnCancel.addActionListener(e -> {
            result = null;
            setVisible(false);
        });

        // Enter key support
        getRootPane().setDefaultButton(btnLogin);

        pack();
        setLocationRelativeTo(owner);
        setResizable(false);
        
        // Focus on username field
        SwingUtilities.invokeLater(() -> txtUser.requestFocus());
    }

    private User authenticateUser(String username, String password) {
        try {
            // Check credentials with UserManager
            if (userManager.authenticateUser(username, password)) {
                // Get user details from database
                User user = db.getUserByUsername(username);
                if (user != null) {
                    System.out.println("✓ Usuario '" + username + "' autenticado exitosamente");
                    return user;
                }
            }
            
            System.out.println("✗ Autenticación falló para usuario '" + username + "'");
            return null;
        } catch (Exception e) {
            System.err.println("Error de autenticación en base de datos: " + e.getMessage());
            JOptionPane.showMessageDialog(this, 
                "Error de conexión a la base de datos. Por favor intente de nuevo.", 
                "Error del Sistema", 
                JOptionPane.ERROR_MESSAGE);
            return null;
        }
    }

    public static User showLogin(Frame owner) {
        LoginDialog dialog = new LoginDialog(owner);
        dialog.setVisible(true);
        
        // Close database connections when dialog is disposed
        if (dialog.db != null) {
            dialog.db.close();
        }
        if (dialog.userManager != null) {
            dialog.userManager.close();
        }
        
        return dialog.result;
    }
}
