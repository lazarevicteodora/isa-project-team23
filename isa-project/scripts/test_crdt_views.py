import requests
import time
import random

# Load balancer URL
LOAD_BALANCER = "http://localhost:80"
VIDEO_ID = 29

def check_server():
    """Proveri da li server radi"""
    try:
        response = requests.get(f"{LOAD_BALANCER}/api/videos", timeout=3)
        if response.status_code in [200, 401, 403]:  # Bilo koji odgovor znači da server radi
            print("✅ Server je dostupan")
            return True
        else:
            print(f"⚠️ Server status: {response.status_code}")
            return False
    except requests.exceptions.ConnectionError:
        print("❌ Ne mogu da se povežem na server!")
        print("⚠️ Pokreni: docker-compose up -d")
        return False
    except Exception as e:
        print(f"❌ Greška: {e}")
        return False

def send_view_requests(num_requests=100):
    """Šalje zahteve za increment view count"""
    print(f"🚀 Šaljem {num_requests} zahteva...")

    success = 0
    failed = 0

    for i in range(num_requests):
        try:
            response = requests.post(
                f"{LOAD_BALANCER}/api/videos/{VIDEO_ID}/view-crdt",
                timeout=5
            )

            if response.status_code == 200:
                success += 1
                replica = response.text  # Server će vratiti koja replika je odgovorila
                if (i + 1) % 10 == 0:
                    print(f"✅ Request {i+1}/{num_requests}: {replica}")
            else:
                failed += 1
                if failed <= 3:  # Prikaži samo prve 3 greške
                    print(f"⚠️ Request {i+1} - Status: {response.status_code}")

        except Exception as e:
            failed += 1
            if failed <= 3:
                print(f"❌ Request {i+1} FAILED: {e}")

        time.sleep(0.05)  # 50ms pauza

    print(f"\n📊 POSLATO: {success} uspešno, {failed} neuspešno")
    return success, failed

def get_total_views():
    """Dobavi ukupan broj pregleda sa CRDT merge-om"""
    try:
        response = requests.get(f"{LOAD_BALANCER}/api/videos/{VIDEO_ID}/views-crdt", timeout=10)

        print(f"📡 Status: {response.status_code}")

        if response.status_code != 200:
            print(f"❌ Endpoint vratio grešku: {response.status_code}")
            print(f"📄 Response: {response.text[:200]}")
            return None

        try:
            data = response.json()
            return data
        except Exception as e:
            print(f"⚠️ Response nije JSON!")
            print(f"📄 Raw response: {response.text[:200]}")
            return None

    except requests.exceptions.ConnectionError:
        print("❌ Ne mogu da se povežem na server!")
        print("⚠️ Pokreni: docker-compose up -d")
        return None
    except Exception as e:
        print(f"❌ Greška: {e}")
        return None

if __name__ == "__main__":
    print("=" * 50)
    print("🧪 CRDT VIEW COUNT TEST")
    print("=" * 50)
    print()

    # Proveri da li server radi
    if not check_server():
        print("\n⚠️ Server nije dostupan. Test se prekida.")
        exit(1)

    print()
    print("=" * 50)
    print("📊 POČETNO STANJE")
    print("=" * 50)

    # Dobavi početno stanje
    initial = get_total_views()
    if initial:
        print(f"Početno: {initial}")
    else:
        print("⚠️ Ne mogu da pročitam početno stanje")
        print("⚠️ Proveri da li video sa ID={VIDEO_ID} postoji")
        exit(1)

    print()
    print("=" * 50)
    print("🚀 SLANJE ZAHTEVA")
    print("=" * 50)

    # Pošalji zahteve
    success, failed = send_view_requests(100)

    # Sačekaj malo za sinhronizaciju
    print("\n⏳ Čekam 5 sekundi za sinhronizaciju...")
    time.sleep(5)

    print()
    print("=" * 50)
    print("📊 FINALNO STANJE")
    print("=" * 50)

    # Dobavi total
    final = get_total_views()

    if final:
        print(f"\n🎯 REZULTAT:")
        print(f"  Početno: {initial.get('totalViews', 0)}")
        print(f"  Finalno: {final.get('totalViews', 0)}")
        print(f"  Razlika: +{final.get('totalViews', 0) - initial.get('totalViews', 0)}")
        print(f"  Poslato: {success} zahteva")

        diff = final.get('totalViews', 0) - initial.get('totalViews', 0)
        if diff == success:
            print("\n✅ TAČAN BROJ - Svi zahtevi su registrovani!")
        elif diff > 0:
            print(f"\n⚠️ Očekivano +{success}, dobijeno +{diff}")
            print("   (Neki zahtevi možda nisu uspeli ili je bilo race conditions)")
        else:
            print("\n❌ View count se nije promenio!")
    else:
        print("\n❌ Ne mogu da pročitam finalno stanje")

    print("\n" + "=" * 50)