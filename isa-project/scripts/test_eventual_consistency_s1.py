import requests
import time

# URLs
REPLICA_1_URL = "http://localhost:8081"
REPLICA_2_URL = "http://localhost:8082"
VIDEO_ID = 29

def header(title):
    print("\n" + "=" * 80)
    print(f"🧪 {title}")
    print("=" * 80)

def check_health():
    """Proveri da li replike rade"""
    try:
        r1 = requests.get(f"{REPLICA_1_URL}/api/videos", timeout=5)
        r2 = requests.get(f"{REPLICA_2_URL}/api/videos", timeout=5)

        if r1.status_code in [200, 401, 403] and r2.status_code in [200, 401, 403]:
            print("✅ Replica 1: UP")
            print("✅ Replica 2: UP")
            return True
        else:
            print("❌ Neka replika nije dostupna")
            return False
    except Exception as e:
        print(f"❌ Greška pri health check: {e}")
        print("⚠️ Pokreni: docker-compose up -d")
        return False

def get_view_count(replica_url, replica_name):
    """Dobavi view count sa replike"""
    try:
        response = requests.get(
            f"{replica_url}/api/videos/{VIDEO_ID}/views-crdt",
            timeout=10
        )

        if response.status_code != 200:
            print(f"⚠️ {replica_name} - Status: {response.status_code}")
            print(f"   Response: {response.text[:100]}")
            return None

        try:
            data = response.json()
            total = data.get("totalViews", 0)
            print(f"📊 {replica_name}: {total} pregleda")
            return total
        except:
            print(f"⚠️ {replica_name} - Response nije JSON: {response.text[:100]}")
            return None

    except requests.exceptions.Timeout:
        print(f"⚠️ {replica_name} - Timeout")
        return None
    except Exception as e:
        print(f"❌ {replica_name} greška: {e}")
        return None

def send_requests(replica_url, replica_name, count):
    """Pošalji 'count' zahteva na repliku"""
    print(f"\n🚀 Šaljem {count} zahteva na {replica_name}...")

    success = 0
    failed = 0

    for i in range(count):
        try:
            response = requests.post(
                f"{replica_url}/api/videos/{VIDEO_ID}/view-crdt",
                timeout=5
            )
            if response.status_code == 200:
                success += 1
                if (i + 1) % 10 == 0:
                    print(f"  ✅ {i + 1}/{count} poslato")
            else:
                failed += 1
                if failed <= 3:  # Prikaži samo prve 3 greške
                    print(f"  ⚠️ Request {i + 1} - Status: {response.status_code}")
        except Exception as e:
            failed += 1
            if failed <= 3:
                print(f"  ❌ Request {i + 1} neuspešan: {e}")

    print(f"✅ {replica_name}: {success}/{count} zahteva uspešno")
    if failed > 0:
        print(f"⚠️ {replica_name}: {failed}/{count} zahteva neuspešno")

    return success

def main():
    header("STUDENT 1 - EVENTUAL CONSISTENCY TEST SCENARIO")

    print("\n📋 SCENARIO:")
    print("1. Pošalji 50 zahteva direktno na Repliku 1")
    print("2. Pošalji 50 zahteva direktno na Repliku 2")
    print("3. Čitaj brojač sa obe replike PRE sinhronizacije")
    print("4. Sačekaj periodic sync (30s)")
    print("5. Čitaj brojač sa obe replike POSLE sinhronizacije")

    # Health check
    header("PRE-CHECK: HEALTH STATUS")
    if not check_health():
        print("\n❌ Replike nisu dostupne!")
        print("Pokreni: docker-compose up -d")
        return

    input("\n⏸️  Pritisni ENTER za početak testa...")

    # ===================================================================
    # KORAK 1: Pošalji 50 zahteva na Repliku 1
    # ===================================================================
    header("KORAK 1/5: Slanje 50 zahteva na REPLIKU 1")
    r1_sent = send_requests(REPLICA_1_URL, "Replica 1", 50)

    # ===================================================================
    # KORAK 2: Pošalji 50 zahteva na Repliku 2
    # ===================================================================
    header("KORAK 2/5: Slanje 50 zahteva na REPLIKU 2")
    r2_sent = send_requests(REPLICA_2_URL, "Replica 2", 50)

    # ===================================================================
    # KORAK 3: Čitaj brojač PRE sinhronizacije
    # ===================================================================
    header("KORAK 3/5: Čitanje brojača PRE SINHRONIZACIJE")

    print("\n⏱️  Kratka pauza (2s) pre čitanja...")
    time.sleep(2)

    print("\n📖 Čitam stanje sa obe replike...")
    r1_before = get_view_count(REPLICA_1_URL, "Replica 1")
    r2_before = get_view_count(REPLICA_2_URL, "Replica 2")

    if r1_before is None or r2_before is None:
        print("\n❌ Ne mogu da pročitam stanje replika!")
        print("⚠️ Proveri da li video sa ID={VIDEO_ID} postoji")
        print("⚠️ Proveri logove: docker-compose logs backend1")
        return

    print(f"\n📊 REZULTAT PRE SYNC:")
    print(f"  Replica 1: {r1_before}")
    print(f"  Replica 2: {r2_before}")
    print(f"  Razlika: {abs(r1_before - r2_before)}")

    if r1_before == r2_before:
        print("✅ VEĆ SINHRONIZOVANO (push/pull sync radio brzo!)")
    else:
        print("⏳ Razlika postoji - čekam periodic sync...")

    # ===================================================================
    # KORAK 4: Čekaj periodic sync (30s)
    # ===================================================================
    header("KORAK 4/5: Čekanje PERIODIC SYNC (30 sekundi)")

    print("⏰ Periodic sync radi na svakih 30s...")
    print("⏳ Čekam 35 sekundi da se osiguram da je sync prošao...")

    for i in range(35, 0, -5):
        print(f"  ⏱️  {i} sekundi preostalo...")
        time.sleep(5)

    print("✅ Periodic sync trebalo bi da je završio!\n")

    # ===================================================================
    # KORAK 5: Čitaj brojač POSLE sinhronizacije
    # ===================================================================
    header("KORAK 5/5: Čitanje brojača POSLE SINHRONIZACIJE")

    print("📖 Čitam stanje sa obe replike...")
    r1_after = get_view_count(REPLICA_1_URL, "Replica 1")
    r2_after = get_view_count(REPLICA_2_URL, "Replica 2")

    if r1_after is None or r2_after is None:
        print("\n❌ Ne mogu da pročitam stanje replika nakon sync-a!")
        return

    print(f"\n📊 REZULTAT POSLE SYNC:")
    print(f"  Replica 1: {r1_after}")
    print(f"  Replica 2: {r2_after}")
    print(f"  Razlika: {abs(r1_after - r2_after)}")

    # ===================================================================
    # FINALNI IZVEŠTAJ
    # ===================================================================
    header("📊 FINALNI IZVEŠTAJ")

    print(f"""
📤 POSLATO:
  - Replica 1: {r1_sent} zahteva
  - Replica 2: {r2_sent} zahteva
  - UKUPNO: {r1_sent + r2_sent} zahteva

📊 STANJE PRE SYNC:
  - Replica 1: {r1_before} pregleda
  - Replica 2: {r2_before} pregleda
  - Razlika: {abs(r1_before - r2_before)}

📊 STANJE POSLE SYNC:
  - Replica 1: {r1_after} pregleda
  - Replica 2: {r2_after} pregleda
  - Razlika: {abs(r1_after - r2_after)}
""")

    # Provera eventual consistency
    if r1_after == r2_after:
        print("✅ EVENTUAL CONSISTENCY POSTIGNUTA!")
        print(f"✅ Obe replike imaju {r1_after} pregleda")

        expected = r1_sent + r2_sent
        if r1_after >= expected:
            print(f"✅ BROJ ODGOVARA: {r1_after} >= {expected}")
        else:
            print(f"⚠️ Očekivano: {expected}, Dobijeno: {r1_after}")
            print("   (Mogući razlog: neki zahtevi nisu uspeli)")
    else:
        diff = abs(r1_after - r2_after)
        print(f"⚠️ RAZLIKA JOŠ POSTOJI: {diff} pregleda")
        print("   (Periodic sync možda još nije stigao ili je async)")

    print("\n" + "=" * 80)
    print("✅ TEST ZAVRŠEN")
    print("=" * 80)

if __name__ == "__main__":
    main()