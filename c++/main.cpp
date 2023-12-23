#include <QApplication>
#include <QWidget>
#include <QGridLayout>
#include <QFrame>
#include <QLineEdit>
#include <QLabel>
#include <QPushButton>
#include <QComboBox>
#include <QString>
#include <vector>
#include <QFile>
#include <QJsonDocument>
#include <QJsonObject>
#include <QJsonArray>

class Task {
private:
    QString name;
    QString date;
    QString importance;
    int id;
public:
    Task(const QString &name, const QString &date, const QString &importance, int id) {
        this->name = name;
        this->date = date;
        this->importance = importance;
        this->id = id;
    }

    const QString &getName() const {
        return name;
    }


    const QString &getDate() const {
        return date;
    }


    const QString &getImportance() const {
        return importance;
    }


    const int getId() const {
        return id;
    }

    void setId(int i) {
        this->id = i;
    }

};

class Data {
private:
    std::vector<Task> tasks;
public:
    Data() {
        readData();
    }

private:
    void readData() {
        QFile file("/Users/simonesamardzhiev/Desktop/My projects/Task Manager/c++/data.json");
        if (!file.open(QIODevice::ReadOnly)) {
            qDebug() << "The file in read data couldn't be opened";
            return;
        }
        QByteArray array = file.readAll();
        file.close();

        QJsonDocument jsonDocument = QJsonDocument::fromJson(array);
        QJsonArray jsonArray = jsonDocument.array();

        for (const QJsonValueRef &value: jsonArray) {
            QJsonObject object = value.toObject();
            tasks.emplace_back(object.value("name").toString(), object.value("date").toString(),
                               object.value("importance").toString(), object.value("id").toInt());
        }
    }

    void reindex() {
        for (int i = 0; i < tasks.size(); i++) {
            tasks[i].setId(i);
        }
    }

    int getId() {
        if (tasks.empty()) {
            return 0;
        } else {
            return tasks.back().getId() + 1;
        }
    }

public:
    void writeData() {
        reindex();
        QJsonArray jsonArray;
        for (const Task &task: tasks) {
            QJsonObject object;
            object["name"] = task.getName();
            object["date"] = task.getDate();
            object["importance"] = task.getImportance();
            object["id"] = task.getId();
            jsonArray.append(object);
        }
        QJsonDocument jsonDocument(jsonArray);
        QByteArray byteArray = jsonDocument.toJson();

        QFile file("/Users/simonesamardzhiev/Desktop/My projects/Task Manager/c++/data.json");
        if (!file.open(QFile::WriteOnly | QFile::Text)) {
            qDebug() << "The file in write data couldn't be open";
            return;
        }
        file.write(byteArray);
        file.close();
    }

    void deleteTask(int id) {
        for (int i = 0; i < tasks.size(); i++) {
            if (tasks[i].getId() == id) {
                tasks.erase(tasks.begin() + i);
                break;
            }
        }
    }

    void addTask(const QString &name, const QString &date, const QString &importance) {
        tasks.emplace_back(name, date, importance, getId());
    }

    const std::vector<Task> getTasks() const {
        return tasks;
    }
};

class AddTaskWindow : public QWidget {
Q_OBJECT

private:
    QGridLayout *layout = new QGridLayout;
    QLineEdit *nameLine = new QLineEdit;
    QLineEdit *dateLine = new QLineEdit;
    QComboBox *importanceComboBox = new QComboBox;

public:
    explicit AddTaskWindow(QWidget *parent = nullptr) : QWidget(nullptr) {
        this->setWindowTitle("Add new task");
        this->setGeometry(200, 200, 500, 500);

        importanceComboBox->addItem("Not important");
        importanceComboBox->addItem("Important");
        importanceComboBox->addItem("must");

        auto *button = new QPushButton("Add");
        connect(button, &QPushButton::clicked, [=]() {
            onAddClicked();
        });

        layout->addWidget(new QLabel("Enter the name :"), 0, 0);
        layout->addWidget(nameLine, 0, 1);
        layout->addWidget(new QLabel("Enter the date :"), 1, 0);
        layout->addWidget(dateLine, 1, 1);
        layout->addWidget(importanceComboBox, 2, 0);
        layout->addWidget(button, 3, 0);

        this->setLayout(layout);
    }

signals:

    void taskAdded(const QString &name, const QString &date, const QString &importance);

public slots:

    void onAddClicked() {
        QString name = nameLine->text();
        QString date = dateLine->text();
        QString importance = importanceComboBox->currentText();

        if (importance == "Not important") {
            importance = "green";
        } else if (importance == "Important") {
            importance = "orange";
        } else {
            importance = "red";
        }
        emit taskAdded(name, date, importance);
    };
};

class Window : public QWidget {
Q_OBJECT

public:
    Data data;
    QGridLayout *layout = new QGridLayout;
    std::vector<QFrame *> tasks;
public:
    explicit Window(QWidget *parent = nullptr) : QWidget(parent) {
        // setting attributes to the window;
        this->setWindowTitle("Task Manager");
        this->setGeometry(100, 100, 600, 600);

        // creating widget
        auto *button = new QPushButton("Add task");
        connect(button, &QPushButton::clicked, [=]() {
            auto *taskWindow = new AddTaskWindow;
            connect(taskWindow, &AddTaskWindow::taskAdded, this, &Window::addTask);
            taskWindow->show();
        });
        renderTasks();
        layout->addWidget(button, 0, 0);

        this->setLayout(layout);

    }

    void addTask(const QString &name, const QString &date, const QString &importance) {
        data.addTask(name, date, importance);
        renderTasks();
    }

private:
    void renderTasks() {
        for (QFrame *frame: tasks) {
            layout->removeWidget(frame);
            delete frame;
        }
        tasks.clear();

        int row = 1;
        for (const Task &task: data.getTasks()) {
            auto *frameLayout = new QGridLayout;
            auto *frame = new QFrame;

            auto *label = new QLabel;
            label->setText("Name : " + task.getName() + "\n Date : " + task.getDate());
            label->setStyleSheet(QString("color: %1;").arg(task.getImportance()));

            auto *button = new QPushButton("Delete");
            int id = task.getId();
            connect(button, &QPushButton::clicked, [this, id]() {
                onDeleteClicked(id);
                renderTasks();
            });

            frameLayout->setContentsMargins(0, 0, 0, 0);
            frameLayout->addWidget(label, 0, 0);
            frameLayout->addWidget(button, 0, 1);

            frame->setLayout(frameLayout);
            tasks.push_back(frame);

            layout->addWidget(frame, row++, 0);
        }
    }

    void closeEvent(QCloseEvent *event) override {
        data.writeData();
        QWidget::closeEvent(event);
    }

protected:

private slots:

    void onDeleteClicked(int id) {
        data.deleteTask(id);
        renderTasks();
    }
};

int main(int argc, char *argv[]) {
    QApplication application(argc, argv);
    Window window;
    window.show();
    return QApplication::exec();
}

#include "main.moc"