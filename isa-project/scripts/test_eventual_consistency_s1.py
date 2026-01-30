import requests
import time

# URLs
REPLICA_1_URL = "http://localhost:8081"
REPLICA_2_URL = "http://localhost:8082"
VIDEO_ID = 1

def header(title):
    print("\n" + "=" * 80)
    print(f"🧪 {title}")
    print("=" * 80)

def get_view_count(replica_url, replica_name):
    """Dobavi view count sa replike"""
    try:
        response = requests.get(
            f"{replica_url}/api/videos/{VIDEO_ID}/views-crdt",
            timeout=5
        )
        total = response.json().get("totalViews", 0)
        print(f"📊 {replica_name}: {total} pregleda")
        return total
    except Exception as e:
        print(f"❌ {replica_name} greška: {e}")
        return 0

def send_requests(replica_url, replica_name, count):
    """Pošalji 'count' zahteva na repliku"""
    print(f"\n🚀 Šaljem {count} zahteva na {replica_name}...")

    success = 0
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
        except Exception as e:
            print(f"  ❌ Zahtev {i + 1} neuspešan: {e}")

    print(f"✅ {replica_name}: {success}/{count} zahteva uspešno")
    return success

def main():
    header("STUDENT 1 - EVENTUAL CONSISTENCY TEST SCENARIO")

    print("\n📝 SCENARIO:")
    print("1. Pošalji 50 zahteva direktno na Repliku 1")
    print("2. Pošalji 50 zahteva direktno na Repliku 2")
    print("3. Čitaj brojač sa obe replike PRE sinhronizacije")
    print("4. Sačekaj periodic sync (30s)")
    print("5. Čitaj brojač sa obe replike POSLE sinhronizacije")

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

        if r1_after == (r1_sent + r2_sent):
            print(f"✅ TAČAN BROJ: {r1_after} = {r1_sent} + {r2_sent}")
        else:
            print(f"⚠️  Očekivano: {r1_sent + r2_sent}, Dobijeno: {r1_after}")
            print("   (Mogući razlog: video već imao neke preglede)")
    else:
        print(f"⚠️  RAZLIKA JOŠ POSTOJI: {abs(r1_after - r2_after)} pregleda")
        print("   (Periodic sync možda još nije stigao)")

    print("\n" + "=" * 80)
    print("✅ TEST ZAVRŠEN")
    print("=" * 80)

if __name__ == "__main__":
    main()