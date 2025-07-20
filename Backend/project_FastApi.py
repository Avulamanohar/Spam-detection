import requests
from fastapi import FastAPI
from pydantic import BaseModel
import sys, os

# Ensure we can import from src/
sys.path.append(os.path.abspath(os.path.join(os.path.dirname(__file__), "..", "src")))
from collab_model import classify_message 
from fastapi.middleware.cors import CORSMiddleware

app = FastAPI()

class MessageRequest(BaseModel):
    message: str

# Java API endpoints
JAVA_CHECK_URL = "http://localhost:8080/api/messages/check"
JAVA_SAVE_URL = "http://localhost:8080/api/messages/save"

@app.post("/classify/")
def classify_message_api(data: MessageRequest):
    # 1. Try to query Java backend
    try:
        java_resp = requests.get(
            JAVA_CHECK_URL,
            params={"message": data.message},
            timeout=10 # don't wait forever
        ).json()

        if java_resp.get("verdict") is not None:
            # Found in DB → return directly
            return {"verdict": java_resp["verdict"], "source": "db"}

    except Exception as e:
        print(f"Java backend not available, skipping DB check. Reason: {e}")

    # 
    ml_verdict = classify_message(data.message)

    # 3. Try saving ML result back to DB (if Java is up)
    try:    
        requests.post(
            JAVA_SAVE_URL,
            json={"message": data.message, "verdict": ml_verdict},
            timeout=10
        )
    except Exception as e:
        print(f" Could not save result to Java backend. Reason: {e}")

    #  4. Return ML verdict
    return {"verdict": ml_verdict, "source": "ml"}


#  Enable CORS for frontend
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"], 
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

@app.get("/")
def read_root():
    return {"message": "Hello FastAPI"}

if __name__ == "__main__":
    import uvicorn
    uvicorn.run(app, host="0.0.0.0", port=8000)
