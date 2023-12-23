import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

import java.lang.reflect.Type;
import java.util.ArrayList;


class Task {
    private final String name;
    private final String date;
    private final String importance;
    private int id;

    public Task(String name, String date, String importance, int id) {
        this.name = name;
        this.date = date;
        this.importance = importance;
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public String getDate() {
        return date;
    }

    public String getImportance() {
        return importance;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }
}

class Data {
    private ArrayList<Task> tasks;

    public Data() {
        readData();
    }

    private void readData() {
        try (FileReader fileReader = new FileReader("/Users/simonesamardzhiev/Desktop/My projects/Task Manager/java/data.json")) {
            Gson gson = new Gson();
            Type type = new TypeToken<ArrayList<Task>>() {
            }.getType();
            tasks = gson.fromJson(fileReader, type);
        } catch (IOException e) {
            System.err.println("There was an error in reading the file");
            System.exit(1);
        }
    }

    public void writeData() {
        reindex();
        try (FileWriter fileWriter = new FileWriter("/Users/simonesamardzhiev/Desktop/My projects/Task Manager/java/data.json")) {
            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            gson.toJson(tasks, fileWriter);
        } catch (IOException e) {
            System.err.println("There was an error writing into the file");
            System.exit(1);
        }
    }

    private void reindex() {
        for (int i = 0; i < tasks.size(); i++) {
            tasks.get(i).setId(i);
        }
    }

    private int getAvailableIndex() {
        if (tasks.isEmpty()) {
            return 0;
        } else {
            return tasks.getLast().getId() + 1;
        }
    }

    public void addTask(String name, String date, String importance) {
        tasks.add(new Task(name, date, importance, getAvailableIndex()));
    }

    public void deleteTask(int id) {
        for (Task task : tasks) {
            if (task.getId() == id) {
                tasks.remove(task);
                break;
            }
        }
    }

    ArrayList<Task> getTasks() {
        return tasks;
    }
}

class AddTaskWindow extends JFrame {
    private final JTextField textName = new JTextField();
    private final JTextField textDate = new JTextField();

    private final JComboBox<String> importance = new JComboBox<>();
    private Window window;

    public AddTaskWindow(Window window) {
        super();
        this.setTitle("Add new task");
        this.setSize(300, 300);
        this.setLayout(new GridBagLayout());

        this.window = window;

        importance.addItem("Not important");
        importance.addItem("Important");
        importance.addItem("Must");

        textName.setPreferredSize(new Dimension(100, 30));
        textDate.setPreferredSize(new Dimension(100, 30));

        GridBagConstraints layout = new GridBagConstraints();
        layout.gridx = 0;
        layout.gridy = 0;
        this.add(new JLabel("Enter the name :"), layout);

        layout.gridx = 1;
        this.add(textName, layout);

        layout.gridy = 1;
        layout.gridx = 0;
        this.add(new JLabel("Enter the date :"), layout);

        layout.gridx = 1;
        this.add(textDate, layout);

        layout.gridy = 2;
        layout.gridx = 0;
        this.add(importance, layout);

        JButton button = new JButton("Add");
        button.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                String name = AddTaskWindow.this.textName.getText();
                String date = AddTaskWindow.this.textDate.getText();
                String importance = "";
                switch ((String) (AddTaskWindow.this.importance.getSelectedItem())) {
                    case "Not important":
                        importance = "green";
                        break;
                    case "Important":
                        importance = "orange";
                        break;
                    case "Must":
                        importance = "red";
                }
                AddTaskWindow.this.window.addTask(name, date, importance);
                AddTaskWindow.this.dispose();
            }
        });

        layout.gridy = 3;
        this.add(button, layout);

        this.setVisible(true);
    }
}

class Window extends JFrame {
    private final ArrayList<JPanel> tasks = new ArrayList<>();
    private final Data data = new Data();
    private final JButton addButton = new JButton("Add task");

    public Window() {
        super();

        this.setTitle("Task manager");
        this.setSize(600, 600);
        this.setLayout(new GridBagLayout());

        this.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                Window.this.data.writeData();
                System.exit(0);
            }
        });

        GridBagConstraints layout = new GridBagConstraints();
        layout.gridx = 0;
        layout.gridy = 0;


        addButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                new AddTaskWindow(Window.this);
            }
        });
        this.add(addButton, layout);

        renderTasks();
    }

    void renderTasks() {
        this.getContentPane().removeAll();
        tasks.clear();

        GridBagConstraints layout = new GridBagConstraints();
        layout.gridx = 0;
        layout.gridy = 0;
        this.add(addButton, layout);

        layout.gridy = 1;

        for (Task task : data.getTasks()) {
            JPanel panel = new JPanel();
            panel.setLayout(new GridBagLayout());

            GridBagConstraints layoutPanel = new GridBagConstraints();
            layoutPanel.gridx = 0;
            layoutPanel.gridy = 0;

            JLabel label = new JLabel(String.format("<html>Name : %s <br> Date : %s</html>", task.getName(), task.getDate()));
            switch (task.getImportance()) {
                case "green":
                    label.setForeground(Color.green);
                    break;
                case "orange":
                    label.setForeground(Color.orange);
                    break;
                case "red":
                    label.setForeground(Color.red);
            }
            JButton button = new JButton("Delete");
            final int finalId = task.getId();
            button.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent actionEvent) {
                    Window.this.delete_pressed(finalId);
                }
            });

            panel.add(label, layoutPanel);
            layoutPanel.gridx++;
            panel.add(button, layoutPanel);

            this.add(panel, layout);
            layout.gridy++;
        }
        this.getContentPane().repaint();
        this.getContentPane().revalidate();
    }

    private void delete_pressed(int id) {
        data.deleteTask(id);
        renderTasks();
    }

    public void addTask(String name, String date, String importance) {
        data.addTask(name, date, importance);
        renderTasks();
    }
}

public class Main {
    public static void main(String[] args) {
        Window window = new Window();
        window.setVisible(true);
    }
}