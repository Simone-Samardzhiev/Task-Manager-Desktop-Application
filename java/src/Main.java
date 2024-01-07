import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.Type;
import java.sql.*;
import java.sql.Date;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;


record Task(String task, Date date, String importance, int id) { }

class Data implements Iterable<Task> {
    private final ArrayList<Task> tasks = new ArrayList<>();
    private String username;
    private String password;

    public Data() {
        getLoginInfo();
        readData();
    }

    private void getLoginInfo() {
        try (FileReader reader = new FileReader("/Users/simonesamardzhiev/Desktop/My projects/Task Manager/java/login_info.json")) {
            Gson gson = new Gson();
            Type type = new TypeToken<HashMap<String, String>>() {
            }.getType();
            HashMap<String, String> map = gson.fromJson(reader, type);

            username = map.get("username");
            password = map.get("password");
        } catch (IOException e) {
            System.err.println("There was an error in reading the login info");
        }
    }

    private void readData() {
        String url = "jdbc:mysql://localhost:3306/TaskManager";
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            Connection connection = DriverManager.getConnection(url, username, password);
            Statement statement = connection.createStatement();
            String query = "SELECT * FROM JavaData";
            ResultSet resultSet = statement.executeQuery(query);

            while (resultSet.next()) {
                String task = resultSet.getString("task");
                Date date = resultSet.getDate("date");
                String importance = resultSet.getString("importance");
                int id = resultSet.getInt("id");
                tasks.add(new Task(task, date, importance, id));
            }

            connection.close();
            statement.close();
            resultSet.close();

        } catch (Exception e) {
            System.err.println("There was an error getting data from the database");
            System.out.println(e.getMessage());
        }
    }

    public void saveData() {
        String url = "jdbc:mysql://localhost:3306/TaskManager";
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            Connection connection = DriverManager.getConnection(url, username, password);
            Statement statement = connection.createStatement();
            String query = "DELETE FROM JavaData";
            statement.executeUpdate(query);

            PreparedStatement preparedStatement = connection.prepareStatement("INSERT INTO JavaData(task,date,importance) VALUES (?,?,?)");

            for (Task task : tasks) {
                preparedStatement.setString(1, task.task());
                preparedStatement.setDate(2, task.date());
                preparedStatement.setString(3, task.importance());

                preparedStatement.executeUpdate();
            }

            connection.close();
            statement.close();
            preparedStatement.close();

        } catch (Exception e) {
            System.err.println("There was an error saving data in the database");
            System.out.println(e.getMessage());
        }
    }

    public void deleteTask(int id) {
        for (Task task : tasks) {
            if (task.id() == id) {
                tasks.remove(task);
                break;
            }
        }
    }

    public void addTask(String task, Date date, String importance) {
        tasks.add(new Task(task, date, importance, getNewId()));
    }

    private int getNewId() {
        if (tasks.isEmpty()) {
            return 0;
        } else {
            return tasks.getLast().id() + 1;
        }
    }

    @Override
    public Iterator<Task> iterator() {
        return tasks.iterator();
    }

}

class NewTaskWindow extends JFrame {
    private final Window window;
    private final Data data;

    private final JTextField taskTextField;

    private final JTextField dateTextField;
    private final JComboBox<String> importanceOptions;

    public NewTaskWindow(Window window, Data data) {
        super();

        // setting the attributes to the window
        this.setSize(300, 300);
        this.setTitle("New Task");
        this.setLayout(new GridBagLayout());

        // passing the arguments
        this.window = window;
        this.data = data;

        // creating the widgets
        GridBagConstraints layout = new GridBagConstraints();
        taskTextField = new JTextField();
        dateTextField = new JTextField();
        importanceOptions = new JComboBox<>();
        JButton addTaskButton = new JButton("Add task");

        // setting attributes and connecting the widgets
        taskTextField.setPreferredSize(new Dimension(150, 25));
        dateTextField.setPreferredSize(new Dimension(150, 25));
        importanceOptions.addItem("Not important");
        importanceOptions.addItem("Important");
        importanceOptions.addItem("Must");
        addTaskButton.addActionListener(e -> NewTaskWindow.this.onAddTaskCLicked());

        // adding the widgets
        layout.gridx = 0;
        layout.gridy = 0;
        this.add(new JLabel("Task :"), layout);

        layout.gridx = 1;
        this.add(taskTextField, layout);

        layout.gridx = 0;
        layout.gridy = 1;
        this.add(new JLabel("Date (yyyy-MM-dd) :"), layout);

        layout.gridx = 1;
        this.add(dateTextField, layout);

        layout.gridx = 0;
        layout.gridy = 2;
        this.add(importanceOptions, layout);

        layout.gridy = 3;
        this.add(addTaskButton, layout);

        this.setVisible(true);
    }

    private void onAddTaskCLicked() {
        String task = taskTextField.getText();
        String dateText = dateTextField.getText();
        String importance = "";
        java.util.Date date;

        importance = switch ((String) Objects.requireNonNull(importanceOptions.getSelectedItem())) {
            case "Not important" -> "green";
            case "Important" -> "orange";
            case "Must" -> "red";
            default -> importance;
        };

        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");

        try {
            date = dateFormat.parse(dateText);
        } catch (ParseException e) {
            JOptionPane.showMessageDialog(this, "The value in the data in invalid !");
            dateTextField.setText("");
            return;
        }

        data.addTask(task,new java.sql.Date(date.getTime()),importance);
        window.onSearch();
    }
}

class TaskWindow extends JFrame {
    private final Window window;
    private final Data data;

    private final int id;

    public TaskWindow(Window window, Data data, Task task) {
        super();

        // setting attributes to the window
        this.setSize(300, 300);
        this.setTitle("Task info");
        this.setLayout(new GridBagLayout());

        // passing teh arguments
        this.window = window;
        this.data = data;
        this.id = task.id();

        // creating the widgets
        GridBagConstraints layout = new GridBagConstraints();
        JLabel labelTask = new JLabel(String.format("Task : %s", task.task()));
        JLabel labelDate = new JLabel(String.format("Date : %s", task.date().toString()));
        JButton markAsDoneButton = new JButton("Mark as done");

        // connecting the button
        markAsDoneButton.addActionListener(e -> TaskWindow.this.onMarkAsDoneClicked());

        // adding the widgets
        layout.gridx = 0;
        layout.gridy = 0;
        this.add(labelTask, layout);

        layout.gridy = 1;
        this.add(labelDate, layout);

        layout.gridy = 2;
        this.add(markAsDoneButton, layout);

        this.setVisible(true);
    }

    private void onMarkAsDoneClicked() {
        data.deleteTask(id);
        window.onSearch();
        this.dispose();
    }
}

class Window extends JFrame {
    private final Data data = new Data();
    private final JTextField searchBar;
    private final ArrayList<JButton> results = new ArrayList<>();

    public Window() {
        super();

        // setting attributes to the window
        this.setSize(500, 500);
        this.setTitle("Task manager");
        this.setLayout(new GridBagLayout());
        this.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                data.saveData();
                System.exit(0);
            }
        });

        // creating the widgets
        GridBagConstraints layout = new GridBagConstraints();
        JButton addButton = new JButton("Add task");
        searchBar = new JTextField();

        // setting attributes and connecting the widgets
        searchBar.setPreferredSize(new Dimension(150, 25));
        searchBar.addActionListener(e -> Window.this.onSearch());
        addButton.addActionListener(e -> new NewTaskWindow(Window.this, Window.this.data));

        // adding the widgets
        layout.gridx = 0;
        layout.gridy = 0;
        this.add(addButton, layout);

        layout.gridy = 1;
        this.add(searchBar, layout);

    }

    public void onSearch() {
        for (JButton result : results) {
            this.getContentPane().remove(result);
        }
        results.clear();

        String text = searchBar.getText();
        GridBagConstraints layout = new GridBagConstraints();
        layout.gridx = 0;
        layout.gridy = 2;

        for (Task task : data) {
            if (task.task().startsWith(text)) {
                JButton result = new JButton(task.task());
                switch (task.importance()) {
                    case "green":
                        result.setForeground(Color.green);
                        break;
                    case "orange":
                        result.setForeground(Color.orange);
                        break;
                    case "red":
                        result.setForeground(Color.red);
                        break;
                }
                results.add(result);

                result.addActionListener(e -> new TaskWindow(Window.this, Window.this.data, task));

                this.add(result, layout);
                layout.gridy++;
            }
        }

        this.repaint();
        this.revalidate();
    }
}

public class Main {
    public static void main(String[] args) {
        Window window = new Window();
        window.setVisible(true);
    }
}
