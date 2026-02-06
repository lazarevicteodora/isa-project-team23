import requests
import time
import random

# Load balancer URL
LOAD_BALANCER = "http://localhost:80"
VIDEO_ID = 1

def send_view_requests(num_requests=100):
    """Šalje zahteve za increment view count"""
    print(f"🚀 Šaljem {num_requests} zahteva...")

    for i in range(num_requests):
        try:
            response = requests.post(
                f"{LOAD_BALANCER}/api/videos/{VIDEO_ID}/view-crdt",
                timeout=5
            )

            replica = response.text  # Server će vratiti koja replika je odgovorila
            print(f"✅ Request {i+1}: {replica}")

        except Exception as e:
            print(f"❌ Request {i+1} FAILED: {e}")

        time.sleep(0.05)  # 50ms pauza

def get_total_views():
    """Dobavi ukupan broj pregleda sa CRDT merge-om"""
    response = requests.get(f"{LOAD_BALANCER}/api/videos/{VIDEO_ID}/views-crdt")
    return response.json()

if __name__ == "__main__":
    print("=" * 50)
    print("🧪 CRDT VIEW COUNT TEST")
    print("=" * 50)

    # Pošalji zahteve
    send_view_requests(100)

    # Sačekaj malo
    time.sleep(2)

    # Dobavi total
    total = get_total_views()
    print("\n" + "=" * 50)
    print(f"📊 TOTAL VIEWS (CRDT Merged): {total}")
    print("=" * 50)