import requests
import time

# Konfiguracija
REPLICA_1_URL = "http://localhost:8081"
REPLICA_2_URL = "http://localhost:8082"
LOAD_BALANCER_URL = "http://localhost"

VIDEO_ID = 29


def header(title):
    print("\n" + "=" * 70)
    print(f"🧪 {title}")
    print("=" * 70)


def health_check():
    header("TEST 1: HEALTH CHECK REPLIKA")

    try:
        r1 = requests.get(f"{REPLICA_1_URL}/health", timeout=5)
        r2 = requests.get(f"{REPLICA_2_URL}/health", timeout=5)

        if r1.status_code == 200 and r2.status_code == 200:
            print("✅ Replica 1: UP")
            print("✅ Replica 2: UP")
            return True
        else:
            print("❌ Neka replika nije dostupna")
            return False
    except Exception as e:
        print(f"❌ Greška: {e}")
        return False


def push_sync_test():
    header("TEST 2: PUSH-BASED SYNC")

    print("📝 Scenario:")
    print("Inkrement na Replici 1 → async push ka Replici 2")

    r = requests.post(
        f"{REPLICA_1_URL}/api/videos/{VIDEO_ID}/view-crdt",
        timeout=5
    )

    if r.status_code != 200:
        print(f"❌ Inkrement neuspešan ({r.status_code})")
        return False

    print("✅ Inkrement uspešan, čekam push...")
    time.sleep(2)

    r2 = requests.get(
        f"{REPLICA_2_URL}/api/videos/{VIDEO_ID}/views-crdt",
        timeout=10
    )

    total = r2.json().get("totalViews", 0)
    print(f"📊 Replica 2 total views: {total}")

    if total > 0:
        print("✅ PUSH SYNC RADI")
        return True

    print("⚠️ Push možda još nije stigao (async)")
    return False


def pull_sync_test():
    header("TEST 3: PULL-BASED SYNC")

    print("📝 Scenario:")
    print("5 inkremenata na R1 + 3 na R2 → čitanje triggeruje pull")

    for _ in range(5):
        requests.post(f"{REPLICA_1_URL}/api/videos/{VIDEO_ID}/view-crdt", timeout=5)

    for _ in range(3):
        requests.post(f"{REPLICA_2_URL}/api/videos/{VIDEO_ID}/view-crdt", timeout=5)

    time.sleep(2)

    r = requests.get(
        f"{REPLICA_2_URL}/api/videos/{VIDEO_ID}/views-crdt",
        timeout=10
    )

    total = r.json().get("totalViews", 0)
    print(f"📊 Merged total: {total}")

    if total >= 8:
        print("✅ PULL SYNC RADI")
        return True

    print("❌ Pull sync nije dao očekivan rezultat")
    return False


def load_balancer_test():
    header("TEST 4: LOAD BALANCER + EVENTUAL CONSISTENCY")

    print("📝 Scenario:")
    print("20 zahteva preko nginx load balancera")

    for i in range(20):
        requests.post(
            f"{LOAD_BALANCER_URL}/api/videos/{VIDEO_ID}/view-crdt",
            timeout=5
        )
        if (i + 1) % 5 == 0:
            print(f"  ✅ {i + 1}/20 poslato")

    time.sleep(3)

    r1 = requests.get(
        f"{REPLICA_1_URL}/api/videos/{VIDEO_ID}/views-crdt",
        timeout=10
    )
    r2 = requests.get(
        f"{REPLICA_2_URL}/api/videos/{VIDEO_ID}/views-crdt",
        timeout=10
    )

    t1 = r1.json().get("totalViews", 0)
    t2 = r2.json().get("totalViews", 0)

    print(f"📊 Replica 1 total: {t1}")
    print(f"📊 Replica 2 total: {t2}")

    if t1 == t2 and t1 >= 20:
        print("✅ EVENTUAL CONSISTENCY RADI")
        return True

    print("⚠️ Razlika postoji, periodic sync će je ukloniti")
    return False


def main():
    results = []

    if not health_check():
        print("\n❌ Replike nisu dostupne. Pokreni docker-compose.")
        return

    results.append(("Push sync", push_sync_test()))
    results.append(("Pull sync", pull_sync_test()))
    results.append(("Eventual consistency", load_balancer_test()))

    print("\n" + "=" * 70)
    print("📊 FINALNI IZVEŠTAJ")
    print("=" * 70)

    for name, ok in results:
        print(f"{name.ljust(30)} : {'✅ PASS' if ok else '❌ FAIL'}")

    passed = sum(1 for _, ok in results if ok)
    print(f"\n🏆 Rezultat: {passed}/{len(results)} testova prošlo")

    if passed == len(results):
        print("✅ S3 ZAHTEV JE U POTPUNOSTI ISPUNJEN")
    else:
        print("⚠️ Neki testovi nisu prošli zbog async prirode sistema")


if __name__ == "__main__":
    main()
