import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Iterator;

class Task {
    final private String name;
    final private String date;
    final private String importance;
    private int id;

    Task(String name, String date, String importance, int id) {
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

class Data implements Iterable<Task> {
    private ArrayList<Task> list;
    private final String PATH = "/home/simone/Desktop/My projects/Task Manager Desktop Application/java/data.json";

    public Data() {
        this.read_data();
    }

    private void read_data() {
        Type type = new TypeToken<ArrayList<Task>>() {
        }.getType();
        String jsonData = null;

        try {
            jsonData = new String(Files.readAllBytes(Paths.get(PATH)));
        } catch (IOException e) {
            System.err.println("There was an error reading the data");
        }

        list = new Gson().fromJson(jsonData, type);
    }

    private void write_data() {
        Gson gson = new Gson();
        String jsonData = gson.toJson(this.list);

        try {
            Files.write(Paths.get(PATH), jsonData.getBytes());
        } catch (IOException e) {
            System.err.println("There was an error writing the data");
        }
    }

    private class MyIterator implements Iterator<Task> {
        private int position = 0;

        @Override
        public boolean hasNext() {
            return (position < list.size());
        }

        @Override
        public Task next() {
            if (hasNext()) {
                return list.get(position++);
            } else {
                return null;
            }
        }
    }

    @Override
    public Iterator<Task> iterator() {
        return new MyIterator();
    }

    public void add_task(Task task) {
        this.list.add(task);
        this.write_data();
    }

    public void delete_task_by_id(int id) {
        for (int i = 0; i < this.list.size(); i++) {
            if (this.list.get(i).getId() == id) {
                this.list.remove(i);
                break;
            }
        }
        this.update_indexes();
        this.write_data();
    }

    public int get_avaible_id() {
        return this.list.size();
    }

    public void update_indexes() {
        for (int i = 0; i < this.list.size(); i++) {
            this.list.get(i).setId(i);
        }
        this.write_data();
    }
}

class Window extends JFrame {
    private final Data data = new Data();
    private final ArrayList<Label> labels = new ArrayList<>();
    private final ArrayList<Button> buttons = new ArrayList();

    Window() {
        super();
        this.setLayout(new GridBagLayout());
        this.setTitle("Task Manager");
        this.setSize(500, 400);
        this.create_tasks();
        this.render_tasks();

        GridBagConstraints bagConstraints = new GridBagConstraints();
        bagConstraints.gridx = 0;
        bagConstraints.gridy = 3;

        Label label = new Label("Enter the new task (task:date)");
        this.add(label, bagConstraints);

        bagConstraints.gridy = 4;
        TextArea textArea = new TextArea();
        this.add(textArea, bagConstraints);

        bagConstraints.gridy = 5;
        String[] choices = {"must", "important", "not important"};
        JComboBox<String> comboBox = new JComboBox<>(choices);
        this.add(comboBox, bagConstraints);

        bagConstraints.gridy = 6;
        Button button = new Button("Add");
        this.add(button, bagConstraints);

        bagConstraints.gridy = 7;
        Label label_for_error = new Label();
        label_for_error.setForeground(Color.red);
        this.add(label_for_error, bagConstraints);

        button.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String[] attributes = textArea.getText().split(":");
                String importance = switch (comboBox.getSelectedIndex()) {
                    case 0 -> "red";
                    case 1 -> "orange";
                    case 2 -> "green";
                    default -> "black";
                };

                try {
                    String name = attributes[0].trim();
                    String date = attributes[1].trim();

                    label_for_error.setText("");
                    Task task = new Task(name, date, importance, data.get_avaible_id());
                    data.add_task(task);

                    Label label_new = new Label("name : " + name + " date : " + date);
                    label_new.setForeground(get_colour(importance));
                    labels.add(label_new);

                    Button button_new = new Button("Delete");
                    button_new.addActionListener(new ActionListener() {
                        @Override
                        public void actionPerformed(ActionEvent e) {
                            labels.remove(label_new);
                            buttons.remove(button_new);
                            data.delete_task_by_id(task.getId());
                            remove(label_new);
                            remove(button_new);
                            updateUI();
                        }
                    });
                    buttons.add(button_new);

                    GridBagConstraints gridBagConstraints = new GridBagConstraints();
                    gridBagConstraints.gridy = labels.size() - 1;
                    gridBagConstraints.gridx = 0;
                    add(label_new, gridBagConstraints);

                    gridBagConstraints.gridx = 1;
                    add(button_new, gridBagConstraints);
                    updateUI();
                } catch (IndexOutOfBoundsException error) {
                    label_for_error.setText("Invalid input");
                }
            }
        });

        this.setVisible(true);
    }

    private void updateUI() {
        revalidate();
        repaint();
    }

    private Color get_colour(String string) {
        return switch (string) {
            case "red" -> Color.red;
            case "orange" -> Color.orange;
            case "green" -> Color.green;
            default -> Color.black;
        };
    }

    private void create_tasks() {
        for (Task task : this.data) {
            Label label = new Label("name : " + task.getName() + " date : " + task.getDate());
            label.setForeground(get_colour(task.getImportance()));

            Button button = new Button("Delete");
            button.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    labels.remove(label);
                    buttons.remove(button);
                    data.delete_task_by_id(task.getId());
                    remove(label);
                    remove(button);
                    updateUI();
                }
            });

            this.labels.add(label);
            this.buttons.add(button);
        }
    }

    private void render_tasks() {
        GridBagConstraints gridBagConstraints = new GridBagConstraints();
        for (int i = 0; i < this.labels.size(); i++) {
            gridBagConstraints.gridy = i;
            gridBagConstraints.gridx = 0;
            this.add(this.labels.get(i), gridBagConstraints);

            gridBagConstraints.gridx = 1;
            this.add(this.buttons.get(i), gridBagConstraints);
        }
    }
}

public class Main {
    public static void main(String[] args) {
        Window window = new Window();
    }
}
