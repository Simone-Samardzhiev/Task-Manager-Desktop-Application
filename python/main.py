import json
import sys
from functools import partial

from PyQt6.QtGui import QCloseEvent
from PyQt6.QtWidgets import QApplication, QWidget, QLabel, QPushButton, QLineEdit, QGridLayout, QFrame, QComboBox


class Data:
    data: list[dict]

    def __init__(self) -> None:
        self.read_data()

    def read_data(self) -> None:
        with open("/Users/simonesamardzhiev/Desktop/My projects/Task Manager/python/data.json", "r") as file:
            self.data = json.load(file)

    def write_data(self) -> None:
        with open("/Users/simonesamardzhiev/Desktop/My projects/Task Manager/python/data.json", "w") as file:
            json.dump(self.data, file, indent=4)

    def reindex(self) -> None:
        for i in range(len(self.data)):
            self.data[i]['id'] = i

    def get_id(self) -> int:
        try:
            return self.data[-1]['id'] + 1
        except IndexError:
            return 0

    def add_task(self, name: str, date: str, importance: str) -> None:
        self.data.append({
            'name': name,
            'date': date,
            'importance': importance,
            'id': self.get_id()
        })

    def delete_task(self, _id: int) -> None:
        for task in self.data:
            if task['id'] == _id:
                self.data.remove(task)
                break

    def __iter__(self) -> dict:
        for task in self.data:
            yield task


class AddWindow(QWidget):
    nameLineEdit: QLineEdit
    dateLineEdit: QLineEdit
    importanceChoice: QComboBox

    def __init__(self, wnd: "Window") -> None:
        super().__init__()
        self.setWindowTitle("New task")
        self.setGeometry(300, 300, 400, 400)

        self.window = wnd
        self.nameLineEdit = QLineEdit()
        self.dateLineEdit = QLineEdit()
        self.importanceChoice = QComboBox()
        button = QPushButton("Add")
        button.clicked.connect(self.on_button_clicked)

        self.importanceChoice.addItems(["Not important", "Important", "Must"])

        layout = QGridLayout()
        layout.addWidget(QLabel("Name :"), 0, 0)
        layout.addWidget(self.nameLineEdit, 0, 1)
        layout.addWidget(QLabel("Date :"), 1, 0)
        layout.addWidget(self.dateLineEdit, 1, 1)
        layout.addWidget(self.importanceChoice, 2, 0)
        layout.addWidget(button, 3, 0)

        self.setLayout(layout)

    def on_button_clicked(self) -> None:
        name = self.nameLineEdit.text()
        date = self.dateLineEdit.text()
        importance = ""
        match self.importanceChoice.currentText():
            case "Not important":
                importance = "green"
            case "Important":
                importance = "orange"
            case "Must":
                importance = "red"
        self.window.add_task(name, date, importance)
        self.close()


class Window(QWidget):
    data: Data = Data()
    tasks: list[QFrame] = []
    layout: QGridLayout
    addWindow: AddWindow

    def __init__(self):
        super().__init__()
        self.setWindowTitle("Task manager")
        self.setGeometry(250, 300, 600, 600)

        self.layout = QGridLayout()
        button = QPushButton("Add task")
        button.clicked.connect(self.add_clicked)
        self.layout.addWidget(button, 0, 0)
        self.render_tasks()
        self.setLayout(self.layout)

    def render_tasks(self):
        for task in self.tasks:
            self.layout.removeWidget(task)
        self.tasks.clear()

        row = 1
        for task in self.data:
            task_layout = QGridLayout()
            task_frame = QFrame()

            label = QLabel(f"Name : {task['name']} \n Date : {task['date']} ")
            label.setStyleSheet(f"color : {task['importance']};")
            button = QPushButton("Delete")
            button.clicked.connect(partial(self.delete_clicked, task['id']))

            task_layout.addWidget(label, 0, 0)
            task_layout.addWidget(button, 0, 1)

            task_frame.setLayout(task_layout)
            self.tasks.append(task_frame)
            self.layout.addWidget(task_frame, row, 0)
            row += 1

    def delete_clicked(self, _id: int) -> None:
        self.data.delete_task(_id)
        self.render_tasks()

    def add_clicked(self, ) -> None:
        self.addWindow = AddWindow(self)
        self.addWindow.show()

    def add_task(self, name: str, date: str, importance: str) -> None:
        self.data.add_task(name, date, importance)
        self.render_tasks()

    def closeEvent(self, event: QCloseEvent) -> None:
        self.data.write_data()
        event.accept()


if __name__ == '__main__':
    app = QApplication(sys.argv)
    window = Window()
    window.show()
    sys.exit(app.exec())
