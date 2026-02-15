import uvicorn
from fastapi import FastAPI
from tasks.controller import TaskController

app = FastAPI()
controller = TaskController()
app.include_router(controller.router)

if __name__ == "__main__":
    uvicorn.run(app, host="0.0.0.0", port=8000)
