#include <vector>
#include <QFile>
#include <QJsonArray>
#include <QJsonDocument>
#include <QJsonObject>
#include <QWidget>
#include <QPushButton>
#include <QLabel>
#include <QGridLayout>
#include <QApplication>
#include <QLineEdit>
#include <QComboBox>


#define PATH "/home/simone/Desktop/My projects/Task Manager Desktop Application/c++/data.json"

class Task {
private:
    QString task;
    QString date;
    QString importance;
    int id;
public:
    Task(const QString &task, const QString &date, const QString &importance, int id) {
        this->task = task;
        this->date = date;
        this->importance = importance;
        this->id = id;
    }

    [[nodiscard]] const QString &getTask() const {
        return task;
    }


    [[nodiscard]] const QString &getDate() const {
        return date;
    }


    [[nodiscard]] const QString &getImportance() const {
        return importance;
    }


    [[nodiscard]] int getId() const {
        return id;
    }

    void setId(int i) {
        Task::id = i;
    }


};

class Data {
private:
    std::vector<Task> tasks;
public:
    Data() {
        this->read_data();
    }

    void read_data() {
        QFile file(PATH);

        if (!file.open(QIODevice::ReadOnly | QIODevice::Text)) {
            qDebug() << "Failed to open the file";
        }

        QString string_data = file.readAll();
        QJsonDocument jsonDocument = QJsonDocument::fromJson(string_data.toUtf8());
        QJsonArray jsonArray = jsonDocument.array();

        for (const QJsonValue value: jsonArray) {
            QJsonObject object = value.toObject();
            this->tasks.emplace_back(object["task"].toString(), object["date"].toString(),
                                     object["importance"].toString(), object["id"].toInt());
        }

    }

    void write_data() {
        QJsonArray jsonArray;

        for (const Task &task: this->tasks) {
            QJsonObject object;
            object["task"] = task.getTask();
            object["date"] = task.getDate();
            object["importance"] = task.getImportance();
            object["id"] = task.getId();

            jsonArray.append(object);
        }

        QByteArray jsonData(QJsonDocument(jsonArray).toJson());
        QFile file(PATH);

        if (!file.open(QIODevice::WriteOnly)) {
            qDebug() << "Failed to open file for writing";
            return;
        }

        file.write(jsonData);
        file.close();

    }

    void delete_task_by_id(int id) {
        for (int i = 0; i < tasks.size(); i++) {
            if (tasks[i].getId() == id) {
                tasks.erase(tasks.begin() + i);
                break;
            }
        }
        this->write_data();
    }

    void write_new_task(const Task &task) {
        this->tasks.push_back(task);
        this->write_data();
    }


    int get_available_id() {
        if (this->tasks.empty()) {
            return 0;
        } else {
            return this->tasks.back().getId() + 1;
        }
    }

    void update_ids() {
        for (int i = 0; i < this->tasks.size(); i++) {
            this->tasks[i].setId(i);
        }
        this->write_data();
    }

    std::vector<Task>::iterator begin() {
        return this->tasks.begin();
    }

    std::vector<Task>::iterator end() {
        return this->tasks.end();
    }

};

Data tasks;

class Window : public QWidget {
Q_OBJECT
private:
    std::vector<QPushButton *> buttons;
    std::vector<QLabel *> labels;
    QGridLayout *layout = new QGridLayout;
    QLabel *label_for_new = new QLabel;
    QLineEdit *lineEdit = new QLineEdit;
    QComboBox *options = new QComboBox;
    QPushButton *button_for_new = new QPushButton;
    QLabel *label_error = new QLabel;
public:
    explicit Window(QWidget *parent = nullptr) : QWidget(parent) {
        this->setGeometry(100, 100, 700, 500);
        this->create_tasks();
        this->render_tasks();

        label_for_new->setText("Enter new task (task:date) :");
        this->layout->addWidget(label_for_new, 0, 3);

        this->layout->addWidget(lineEdit, 0, 4);

        options->addItem("Must!");
        options->addItem("Important");
        options->addItem("Not important");
        this->layout->addWidget(options, 0, 5);

        button_for_new->setText("Create");
        connect(button_for_new, &QPushButton::clicked, this, &Window::add_task);
        this->layout->addWidget(button_for_new, 0, 6);

        label_error->setStyleSheet("QLabel { color : red; }");
        this->layout->addWidget(label_error, 0, 7);
        this->setLayout(this->layout);
    }

    void create_tasks() {
        for (auto &task: tasks) {
            QString name = "name : " + task.getTask() + '\n' + "date : " + task.getDate();

            auto *label = new QLabel;
            label->setText(name);
            label->setStyleSheet("QLabel { color : " + task.getImportance() + "; }");

            auto *button = new QPushButton;
            button->setText("delete");
            connect(button, &QPushButton::clicked, [this, label, button, task] {
                this->delete_task(label, button, task);
            });


            this->labels.push_back(label);
            this->buttons.push_back(button);
        }
    }

    void render_tasks() {
        for (int i = 0; i < this->buttons.size(); i++) {
            this->layout->addWidget(labels[i], i, 0);
            this->layout->addWidget(buttons[i], i, 1);
        }
    }

    void delete_task(QLabel *label, QPushButton *button, const Task &for_delete) {
        this->layout->removeWidget(label);
        this->layout->removeWidget(button);

        for (int i = 0; i < this->buttons.size(); i++) {
            if (this->buttons[i] == button) {
                delete this->buttons[i];
                this->buttons.erase(this->buttons.begin() + i);
            }
            if (this->labels[i] == label) {
                delete this->labels[i];
                this->labels.erase(this->labels.begin() + i);
                break;
            }
        }
        tasks.delete_task_by_id(for_delete.getId());
    }

    void add_task() {
        QStringList attributes = this->lineEdit->text().split(':');

        if (attributes.length() != 2) {
            this->label_error->setText("Invalid input");
        } else {
            this->label_error->setText("");
            QString task = attributes[0];
            QString date = attributes[1];
            QString importance;
            switch (this->options->currentIndex()) {
                case 0: {
                    importance = "red";
                    break;
                }
                case 1: {
                    importance = "orange";
                    break;
                }
                case 2: {
                    importance = "green";
                    break;
                }
            }

            Task t(task, date, importance, tasks.get_available_id());
            tasks.write_new_task(t);

            auto *label = new QLabel;
            label->setText("task : " + task + '\n' + "date : " + date);
            label->setStyleSheet("QLabel { color : " + importance + "; }");

            auto *button = new QPushButton;
            button->setText("delete");
            connect(button, &QPushButton::clicked, [this, label, button, t] {
                this->delete_task(label, button, t);
            });

            this->layout->addWidget(label, (int) this->labels.size(), 0);
            this->layout->addWidget(button, (int) this->buttons.size(), 1);

            this->labels.push_back(label);
            this->buttons.push_back(button);
        }
    }

    void closeEvent(QCloseEvent *event) override {
        tasks.update_ids();
    }
};


int main(int argc, char **argv) {
    QApplication app(argc, argv);

    Window window;
    window.show();

    return QApplication::exec();
}

#include "main.moc"