import aiohttp
from .base import TaskSample

class TaskClient:
    def __init__(self, controller_url: str):
        self.controller_url = controller_url

    async def run_sample(self, task_name: str, sample_data: dict):
        async with aiohttp.ClientSession() as session:
            payload = {"task_name": task_name, "data": sample_data}
            async with session.post(f"{self.controller_url}/start", json=payload) as resp:
                return await resp.json()
