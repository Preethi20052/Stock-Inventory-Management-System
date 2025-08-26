import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.ArrayList;

// ------------------ Product Class ------------------
class Product implements Serializable {
    public String id, name;
    public int quantity;
    public double price;

    public Product(String id, String name, int quantity, double price) {
        this.id = id;
        this.name = name;
        this.quantity = quantity;
        this.price = price;
    }

    @Override
    public String toString() {
        return id + " - " + name;
    }
}

// ------------------ Inventory Class ------------------
class Inventory {
    private ArrayList<Product> products = new ArrayList<>();
    private final String FILE_NAME = "inventory.dat";

    public Inventory() {
        load();
    }

    public void add(Product p) {
        products.add(p);
        save();
    }

    public void delete(String id) {
        products.removeIf(p -> p.id.equals(id));
        save();
    }

    public void updateQuantity(String id, int qty) {
        for (Product p : products) {
            if (p.id.equals(id)) {
                p.quantity = qty;
                break;
            }
        }
        save();
    }

    public boolean reduceStock(String id, int qty) {
        for (Product p : products) {
            if (p.id.equals(id)) {
                if (p.quantity >= qty) {
                    p.quantity -= qty;
                    save();
                    return true;
                } else return false;
            }
        }
        return false;
    }

    public ArrayList<Product> getAll() {
        return products;
    }

    private void save() {
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(FILE_NAME))) {
            oos.writeObject(products);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @SuppressWarnings("unchecked")
    private void load() {
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(FILE_NAME))) {
            products = (ArrayList<Product>) ois.readObject();
        } catch (Exception e) {
            products = new ArrayList<>();
        }
    }
}

// ------------------ Billing Frame ------------------
class BillingFrame extends JFrame {
    private Inventory inventory;
    private DefaultTableModel billModel;
    private JTextField productIdField, qtyField;
    private JLabel totalLabel;
    private double total = 0;

    public BillingFrame(Inventory inv) {
        this.inventory = inv;
        setTitle("New Sale / Billing");
        setSize(500, 400);
        setLocationRelativeTo(null);

        billModel = new DefaultTableModel(new String[]{"Product", "Qty", "Price"}, 0);
        JTable billTable = new JTable(billModel);

        productIdField = new JTextField(10);
        qtyField = new JTextField(5);
        JButton addBtn = new JButton("Add Item");
        addBtn.addActionListener(e -> addItem());

        JPanel inputPanel = new JPanel();
        inputPanel.add(new JLabel("Product ID:"));
        inputPanel.add(productIdField);
        inputPanel.add(new JLabel("Qty:"));
        inputPanel.add(qtyField);
        inputPanel.add(addBtn);

        JButton finishBtn = new JButton("Complete Sale");
        finishBtn.addActionListener(e -> finishSale());

        totalLabel = new JLabel("Total: 0.0");

        JPanel bottomPanel = new JPanel(new BorderLayout());
        bottomPanel.add(totalLabel, BorderLayout.WEST);
        bottomPanel.add(finishBtn, BorderLayout.EAST);

        add(inputPanel, BorderLayout.NORTH);
        add(new JScrollPane(billTable), BorderLayout.CENTER);
        add(bottomPanel, BorderLayout.SOUTH);
    }

    private void addItem() {
        String id = productIdField.getText().trim();
        int qty;
        try {
            qty = Integer.parseInt(qtyField.getText().trim());
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Enter valid quantity");
            return;
        }

        Product found = null;
        for (Product p : inventory.getAll()) {
            if (p.id.equals(id)) {
                found = p;
                break;
            }
        }

        if (found == null) {
            JOptionPane.showMessageDialog(this, "Product not found");
            return;
        }
        if (qty > found.quantity) {
            JOptionPane.showMessageDialog(this, "Insufficient stock! Available: " + found.quantity);
            return;
        }

        // reduce stock
        inventory.reduceStock(id, qty);

        double itemTotal = qty * found.price;
        total += itemTotal;
        billModel.addRow(new Object[]{found.name, qty, itemTotal});
        totalLabel.setText("Total: " + total);

        // low stock alert
        if (found.quantity < 5) {
            JOptionPane.showMessageDialog(this,
                    "Warning: Stock of " + found.name + " is low (" + found.quantity + " left)!");
        }
    }

    private void finishSale() {
        if (billModel.getRowCount() == 0) {
            JOptionPane.showMessageDialog(this, "No items added!");
            return;
        }
        saveBill();
        JOptionPane.showMessageDialog(this, "Sale completed. Bill saved!");
        dispose();
    }

    private void saveBill() {
        File dir = new File("bills");
        if (!dir.exists()) dir.mkdir();
        String billName = "bills/Bill_" + System.currentTimeMillis() + ".txt";
        try (PrintWriter pw = new PrintWriter(new FileWriter(billName))) {
            pw.println("Bill No: " + System.currentTimeMillis());
            pw.println("-------------------------------------");
            for (int i = 0; i < billModel.getRowCount(); i++) {
                pw.println(billModel.getValueAt(i, 0) + "  Qty:" + billModel.getValueAt(i, 1)
                        + "  Price:" + billModel.getValueAt(i, 2));
            }
            pw.println("-------------------------------------");
            pw.println("Total: " + total);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

// ------------------ Sales History Frame ------------------
class SalesHistoryFrame extends JFrame {
    private JTextArea area;

    public SalesHistoryFrame() {
        setTitle("Sales History");
        setSize(400, 300);
        setLocationRelativeTo(null);

        area = new JTextArea();
        area.setEditable(false);
        add(new JScrollPane(area), BorderLayout.CENTER);

        loadBills();
    }

    private void loadBills() {
        File dir = new File("bills");
        if (!dir.exists()) {
            area.setText("No bills found.");
            return;
        }
        File[] files = dir.listFiles();
        if (files == null || files.length == 0) {
            area.setText("No bills found.");
            return;
        }
        StringBuilder sb = new StringBuilder();
        for (File f : files) {
            sb.append("Bill: ").append(f.getName()).append("\n");
        }
        area.setText(sb.toString());
    }
}

// ------------------ Main Dashboard ------------------
class Dashboard extends JFrame {
    private Inventory inventory = new Inventory();
    private DefaultTableModel model;

    public Dashboard() {
        setTitle("Inventory Dashboard");
        setSize(700, 400);
        setDefaultCloseOperation(EXIT_ON_CLOSE);

        model = new DefaultTableModel(new String[]{"ID", "Name", "Quantity", "Price"}, 0);
        JTable table = new JTable(model);
        loadProducts();

        JButton addBtn = new JButton("Add Product");
        addBtn.addActionListener(e -> addProduct());

        JButton updateBtn = new JButton("Update Quantity");
        updateBtn.addActionListener(e -> updateProduct());

        JButton delBtn = new JButton("Delete Product");
        delBtn.addActionListener(e -> deleteProduct());

        JButton saleBtn = new JButton("New Sale");
        saleBtn.addActionListener(e -> new BillingFrame(inventory).setVisible(true));

        JButton historyBtn = new JButton("Sales History");
        historyBtn.addActionListener(e -> new SalesHistoryFrame().setVisible(true));

        JPanel btnPanel = new JPanel();
        btnPanel.add(addBtn);
        btnPanel.add(updateBtn);
        btnPanel.add(delBtn);
        btnPanel.add(saleBtn);
        btnPanel.add(historyBtn);

        add(new JScrollPane(table), BorderLayout.CENTER);
        add(btnPanel, BorderLayout.SOUTH);
    }

    private void loadProducts() {
        model.setRowCount(0);
        for (Product p : inventory.getAll()) {
            model.addRow(new Object[]{p.id, p.name, p.quantity, p.price});
        }
    }

    private void addProduct() {
        JTextField id = new JTextField();
        JTextField name = new JTextField();
        JTextField qty = new JTextField();
        JTextField price = new JTextField();

        Object[] fields = {"ID:", id, "Name:", name, "Quantity:", qty, "Price:", price};
        int opt = JOptionPane.showConfirmDialog(this, fields, "Add Product", JOptionPane.OK_CANCEL_OPTION);
        if (opt == JOptionPane.OK_OPTION) {
            try {
                inventory.add(new Product(id.getText(), name.getText(),
                        Integer.parseInt(qty.getText()), Double.parseDouble(price.getText())));
                loadProducts();
            } catch (NumberFormatException e) {
                JOptionPane.showMessageDialog(this, "Invalid Quantity/Price");
            }
        }
    }

    private void updateProduct() {
        String id = JOptionPane.showInputDialog(this, "Enter Product ID to update quantity:");
        String qty = JOptionPane.showInputDialog(this, "Enter new quantity:");
        if (id != null && qty != null) {
            try {
                inventory.updateQuantity(id, Integer.parseInt(qty));
                loadProducts();
            } catch (NumberFormatException e) {
                JOptionPane.showMessageDialog(this, "Invalid Quantity");
            }
        }
    }

    private void deleteProduct() {
        String id = JOptionPane.showInputDialog(this, "Enter Product ID to delete:");
        if (id != null) {
            inventory.delete(id);
            loadProducts();
        }
    }
}

// ------------------ Login Frame ------------------
public class RetailPOS extends JFrame {
    public RetailPOS() {
        setTitle("Login");
        setSize(300, 150);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        JPanel panel = new JPanel(new GridLayout(3, 2));
        JTextField userField = new JTextField();
        JPasswordField passField = new JPasswordField();

        panel.add(new JLabel("Username:"));
        panel.add(userField);
        panel.add(new JLabel("Password:"));
        panel.add(passField);

        JButton loginBtn = new JButton("Login");
        panel.add(new JLabel());
        panel.add(loginBtn);

        add(panel);

        loginBtn.addActionListener(e -> {
            String user = userField.getText();
            String pass = new String(passField.getPassword());
            if (user.equals("admin") && pass.equals("1234")) {
                dispose();
                new Dashboard().setVisible(true);
            } else {
                JOptionPane.showMessageDialog(this, "Invalid credentials!");
            }
        });

        setVisible(true);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new RetailPOS());
    }
}
