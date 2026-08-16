import pytest
import requests
import os
from deepeval import assert_test
from deepeval.test_case import LLMTestCase
from deepeval.metrics import AnswerCorrectnessMetric

def test_llm_accuracy():
    # URL aus Umgebungsvariable (Standardwert auf lokales LLM gesetzt)
    default_url = "https://llm-r9700.am5.hergeth.cloud:8081/v1"
    base_url = os.getenv("LLM_TARGET_URL", default_url)
    
    # Endpoint-Pfad zusammenbauen
    if "/chat/completions" in base_url:
        endpoint = base_url
    elif base_url.endswith("/v1"):
        endpoint = f"{base_url}/chat/completions"
    else:
        endpoint = f"{base_url.rstrip('/')}/v1/chat/completions"

    input_text = "Was ist die Hauptstadt von Frankreich?"
    
    # Modell kann ebenfalls über Umgebungsvariable gesteuert werden
    model = os.getenv("LLM_MODEL", "gpt-4o-mini")
    
    # 1. Request an das LLM
    payload = {
        "model": model,
        "messages": [{"role": "user", "content": input_text}]
    }
    
    try:
        response = requests.post(endpoint, json=payload, timeout=30)
        response.raise_for_status()
        data = response.json()
        actual_output = data["choices"][0]["message"]["content"]
    except Exception as e:
        pytest.fail(f"API request failed: {e}")

    test_case = LLMTestCase(
        input=input_text,
        actual_output=actual_output,
        expected_output="Paris ist die Hauptstadt von Frankreich."
    )

    # AnswerCorrectness braucht keine Context-Daten
    metric = AnswerCorrectnessMetric(threshold=0.7)

    # Bewertung
    assert_test(test_case, [metric])
