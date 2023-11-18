import json
from tkinter import *
from tkinter.ttk import Combobox

PATH = "data.json"


class Data:
    data: list[dict] = []

    def __init__(self):
        self.read_data()

    def read_data(self):
        with open(PATH, "r") as file:
            self.data = json.load(file)

    def write_data(self):
        with open(PATH, "w") as file:
            json.dump(self.data, file, indent=4)

    def update_ids(self):
        id = 0
        for task in self.data:
            task['id'] = id
            id += 1
        self.write_data()

    def write_task(self, name: str, date: str, importance: str):
        try:
            self.data.append({
                "name": name,
                "date": date,
                "importance": importance,
                "id": self.data[-1]['id'] + 1
            })
        except IndexError:
            self.data.append({
                "name": name,
                "date": date,
                "importance": importance,
                "id": 0
            })
        self.write_data()

    def get_task(self, id: int) -> dict:
        for task in self.data:
            if task['id'] == id:
                return task

    def __iter__(self):
        for task in self.data:
            yield task

    def delete_task(self, id: int):
        self.data = [task for task in self.data if task['id'] != id]
        self.write_data()


Tasks = Data()


class Window(Tk):
    tasks: list[dict] = []

    def __init__(self):
        super().__init__()
        self.protocol("WM_DELETE_WINDOW", self.on_closing)
        self.geometry("500x400")

        Label(self, text="Enter the task and the date (task:date)").grid(row=0, column=3)

        self.entry: Entry = Entry(self)
        self.entry.grid(row=0, column=4)

        self.list: Combobox = Combobox(self, state="readonly")
        self.list.grid(row=0, column=5)
        self.list['values'] = ("very important", "important", "not important")

        self.button: Button = Button(self, text="create new task", command=self.create_new_task)
        self.button.grid(row=0, column=6)

        self.error_label: Label = Label(self)
        self.error_label.grid(row=0, column=7)

        self.create_tasks()
        self.render_tasks()
        self.mainloop()

    def create_tasks(self):
        self.tasks.clear()

        for task in Tasks:
            label_text = f"task :{task['name']} \ndate : {task['date']}"
            label = Label(self, text=label_text, fg=task['importance'])
            button = Button(self, text="Delete", command=lambda id=task['id']: self.delete_task(id))

            pack = {
                "label": label,
                "button": button,
                "id": task['id']
            }

            self.tasks.append(pack)

    def render_tasks(self):
        r = 0
        for task in self.tasks:
            task['label'].grid(row=r, column=0)
            task['button'].grid(row=r, column=1)
            r += 1

    def on_closing(self):
        Tasks.update_ids()
        self.destroy()

    def delete_task(self, id: int):
        for task in self.tasks:
            if task['id'] == id:
                task['label'].destroy()
                task['button'].destroy()
                self.tasks.remove(task)
                break
        Tasks.delete_task(id)
        self.render_tasks()

    def create_new_task(self):
        importance: str = self.list.get()
        attributes: list[str] = self.entry.get().split(':')

        try:
            name: str = attributes[0]
            date: str = attributes[1]

            for task in self.tasks:
                task['label'].destroy()
                task['button'].destroy()

            match importance:
                case "very important":
                    Tasks.write_task(name, date, "red")
                case "important":
                    Tasks.write_task(name, date, "orange")
                case "not important":
                    Tasks.write_task(name, date, "green")

            self.create_tasks()
            self.render_tasks()

        except IndexError:
            self.error_label.config(text="The input is invalid !")
        else:
            self.error_label.config(text="")


if __name__ == "__main__":
    wnd = Window()
