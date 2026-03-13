import uuid
from abc import ABC, abstractmethod
from typing import Any, Dict, List, Optional
from pydantic import BaseModel

class TaskSample(BaseModel):
    id: str = str(uuid.uuid4())
    data: Dict[str, Any]

class Task(ABC):
    @abstractmethod
    async def run(self, sample: TaskSample) -> Any:
        pass
