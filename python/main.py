import json
import mysql.connector
import sys
from datetime import datetime
from functools import partial

from PyQt6.QtGui import QCloseEvent
from PyQt6.QtWidgets import QApplication, QWidget, QLabel, QPushButton, QLineEdit, QGridLayout, QComboBox, QMessageBox


class Data:
    tasks: list[dict] = []
    username: str
    password: str

    def __init__(self) -> None:
        self.get_login_info()
        self.retrieve_data()

    def get_login_info(self) -> None:
        with open("login_info.json", "r") as file:
            data = json.load(file)
            self.username = data["username"]
            self.password = data["password"]

    def retrieve_data(self) -> None:
        connection = None
        cursor = None

        try:
            connection = mysql.connector.connect(host="localhost", user=self.username,
                                                 password=self.password, database="TaskManager")
            cursor = connection.cursor()
            query = "SELECT * FROM PythonData"
            cursor.execute(query)
            data = cursor.fetchall()

            for row in data:
                task = {
                    "id": row[0],
                    "task": row[1],
                    "date": row[2],
                    "importance": row[3]
                }
                self.tasks.append(task)

        except mysql.connector.Error as err:
            print("Error while trying to get data ", err)
            sys.exit(1)
        finally:
            if connection is not None:
                cursor.close()
            if cursor is not None:
                connection.close()

    def save_data(self) -> None:
        connection = None
        cursor = None
        try:
            connection = mysql.connector.connect(host="localhost", user=self.username,
                                                 password=self.password, database="TaskManager")
            cursor = connection.cursor()
            query = "DELETE FROM PythonData"
            cursor.execute(query)

            for task in self.tasks:
                query = "INSERT INTO PythonData(task, date, importance) VALUES (%s, %s, %s)"
                cursor.execute(query, (task["task"], task["date"], task["importance"]))

            connection.commit()

        except mysql.connector.Error as err:
            print("Error while trying to save the data ", err)
            sys.exit(1)
        finally:
            if connection is not None:
                connection.close()
            if cursor is not None:
                cursor.close()

    def delete_task(self, _id: int) -> None:
        for task in self.tasks:
            if task["id"] == _id:
                self.tasks.remove(task)

    def add_task(self, task: str, date: datetime, importance: str) -> None:
        task = {
            "id": self.get_new_id(),
            "task": task,
            "date": date,
            "importance": importance
        }

        self.tasks.append(task)

    def get_new_id(self) -> int:
        try:
            return self.tasks[-1]["id"] + 1
        except IndexError:
            return 0

    def __iter__(self) -> dict:
        for task in self.tasks:
            yield task


class NewTaskWindow(QWidget):
    window: "Window"
    data: Data
    nameLineEdit: QLineEdit
    dateLineEdit: QLineEdit
    importanceOptions: QComboBox

    def __init__(self, wnd: "Window", data: Data) -> None:
        super().__init__()

        # setting attributes to the window
        self.setWindowTitle("New Task")
        self.setGeometry(250, 250, 300, 300)

        # passing the arguments
        self.window = wnd
        self.data = data

        # creating the widgets
        layout = QGridLayout()
        self.nameLineEdit = QLineEdit()
        self.dateLineEdit = QLineEdit()
        self.importanceOptions = QComboBox()
        button_add = QPushButton("Add task")

        # connecting the widgets and setting attributes
        for importance in ["Not important", "Important", "Must"]:
            self.importanceOptions.addItem(importance)
        button_add.clicked.connect(self.on_add_clicked)

        # adding the widgets
        layout.addWidget(QLabel("Task :"), 0, 0)
        layout.addWidget(self.nameLineEdit, 0, 1)
        layout.addWidget(QLabel("Date (yyyy-MM-dd) :"), 1, 0)
        layout.addWidget(self.dateLineEdit, 1, 1)
        layout.addWidget(self.importanceOptions, 2, 0)
        layout.addWidget(button_add, 3, 0)

        self.setLayout(layout)

    def on_add_clicked(self) -> None:
        task = self.nameLineEdit.text()
        text_date = self.dateLineEdit.text()
        importance = self.importanceOptions.currentText()

        if len(task) == 0:
            QMessageBox.warning(self, "Error", "Name can't be empty")
            return

        try:
            date = datetime.strptime(text_date, "%Y-%m-%d")
        except ValueError:
            QMessageBox.warning(self, "Error", "Invalid date format")
            return

        match importance:
            case "Not important":
                importance = "green"
            case "Important":
                importance = "orange"
            case "Must":
                importance = "red"

        self.data.add_task(task, date, importance)
        self.window.on_search()


class TaskInfoWindow(QWidget):
    window: "Window"
    data: Data
    _id: int

    def __init__(self, wnd: "Window", data: Data, task: dict) -> None:
        super().__init__()

        # setting attributes to the window
        self.setWindowTitle("Task Information")
        self.setGeometry(250, 250, 300, 300)
        self._id = task["id"]

        # passing the arguments
        self.window = wnd
        self.data = data

        # creating the widgets
        layout = QGridLayout()
        task_label = QLabel(f"Task: {task['task']}")
        date_label = QLabel(f"Date: {task['date'].strftime('%Y-%m-%d')}")
        button_mark_as_done = QPushButton("Mark as done")

        # connecting the button
        button_mark_as_done.clicked.connect(self.on_mark_as_done_clicked)

        # adding the widgets
        layout.addWidget(task_label, 0, 0)
        layout.addWidget(date_label, 1, 0)
        layout.addWidget(button_mark_as_done, 2, 0)

        self.setLayout(layout)

    def on_mark_as_done_clicked(self) -> None:
        self.data.delete_task(self._id)
        self.window.on_search()
        self.close()


class Window(QWidget):
    data = Data()
    layout: QGridLayout
    searchBar: QLineEdit
    results: list[QPushButton] = []
    taskInfoWindow: TaskInfoWindow
    newTaskWindow: NewTaskWindow

    def __init__(self) -> None:
        super().__init__()

        # setting attributes to the window
        self.setGeometry(300, 300, 500, 500)
        self.setWindowTitle("Task Manager")

        # creating the widgets
        self.layout = QGridLayout()
        button_add = QPushButton("Add")
        self.searchBar = QLineEdit()

        # connecting the widgets
        button_add.clicked.connect(self.on_button_add_clicked)
        self.searchBar.returnPressed.connect(self.on_search)

        # adding the widgets
        self.layout.addWidget(button_add, 0, 0)
        self.layout.addWidget(self.searchBar, 1, 0)

        self.setLayout(self.layout)

    def on_button_add_clicked(self) -> None:
        self.newTaskWindow = NewTaskWindow(self, self.data)
        self.newTaskWindow.show()

    def on_search(self) -> None:
        for result in self.results:
            self.layout.removeWidget(result)
        self.results.clear()

        text = self.searchBar.text()
        row = 2

        for task in self.data:
            if task["task"].startswith(text):
                result = QPushButton(task["task"])
                result.setStyleSheet(f"color:{task['importance']};")
                self.results.append(result)

                result.clicked.connect(partial(self.on_result_clicked, task))

                self.layout.addWidget(result, row, 0)
                row += 1

    def on_result_clicked(self, task: dict) -> None:
        self.taskInfoWindow = TaskInfoWindow(self, self.data, task)
        self.taskInfoWindow.show()

    def closeEvent(self, event: QCloseEvent) -> None:
        self.data.save_data()
        event.accept()


if __name__ == "__main__":
    app = QApplication(sys.argv)
    window = Window()
    window.show()
    app.exec()
