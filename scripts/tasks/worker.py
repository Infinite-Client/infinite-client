import asyncio
from fastapi import APIRouter, HTTPException
from .base import Task, TaskSample

class TaskWorker:
    def __init__(self, task: Task):
        self.task = task
        self.router = APIRouter()
        self.router.add_api_route("/run", self.run_task, methods=["POST"])

    async def run_task(self, sample: TaskSample):
        try:
            result = await self.task.run(sample)
            return {"status": "success", "result": result}
        except Exception as e:
            raise HTTPException(status_code=500, detail=str(e))
