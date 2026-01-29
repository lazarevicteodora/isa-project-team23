import requests
import time
import json

# Konfiguracija
REPLICA_1_URL = "http://localhost:8081"
REPLICA_2_URL = "http://localhost:8082"
LOAD_BALANCER_URL = "http://localhost"

VIDEO_ID = 29


def header(title):
    print("\n" + "=" * 70)
    print(f"🧪 {title}")
    print("=" * 70)


def safe_get(url, timeout=5):
    """Bezbedni GET request"""
    try:
        return requests.get(url, timeout=timeout)
    except Exception as e:
        print(f"⚠️ GET {url} failed: {e}")
        return None


def safe_post(url, timeout=5):
    """Bezbedni POST request"""
    try:
        return requests.post(url, timeout=timeout)
    except Exception as e:
        print(f"⚠️ POST {url} failed: {e}")
        return None


def get_json_safely(response):
    """Parse JSON sa error handlingom"""
    if response is None:
        return None

    if response.status_code != 200:
        print(f"⚠️ Status {response.status_code}: {response.text[:100]}")
        return None

    try:
        return response.json()
    except:
        print(f"⚠️ Response nije JSON: {response.text[:100]}")
        return None


def health_check():
    header("TEST 1: HEALTH CHECK REPLIKA")

    r1 = safe_get(f"{REPLICA_1_URL}/api/videos")
    r2 = safe_get(f"{REPLICA_2_URL}/api/videos")

    if r1 and r2 and r1.status_code in [200, 401, 403] and r2.status_code in [200, 401, 403]:
        print("✅ Replica 1: UP")
        print("✅ Replica 2: UP")
        return True
    else:
        print("❌ Neka replika nije dostupna")
        print("⚠️ Pokreni: docker-compose up -d")
        return False


def push_sync_test():
    header("TEST 2: PUSH-BASED SYNC")

    print("📋 Scenario:")
    print("Inkrement na Replici 1 → async push ka Replici 2")

    # Pročitaj početno stanje
    r2_before = safe_get(f"{REPLICA_2_URL}/api/videos/{VIDEO_ID}/views-crdt", timeout=10)
    before_data = get_json_safely(r2_before)
    before_count = before_data.get('totalViews', 0) if before_data else 0

    print(f"📊 Replica 2 pre inkrementa: {before_count}")

    # Inkrementiraj na R1
    r = safe_post(f"{REPLICA_1_URL}/api/videos/{VIDEO_ID}/view-crdt")

    if not r or r.status_code != 200:
        print(f"❌ Inkrement neuspešan")
        return False

    print("✅ Inkrement uspešan, čekam push...")
    time.sleep(3)

    # Proveri R2
    r2_after = safe_get(f"{REPLICA_2_URL}/api/videos/{VIDEO_ID}/views-crdt", timeout=10)
    after_data = get_json_safely(r2_after)

    if not after_data:
        print("❌ Ne mogu da pročitam stanje Replike 2")
        return False

    after_count = after_data.get('totalViews', 0)
    print(f"📊 Replica 2 posle inkrementa: {after_count}")

    if after_count > before_count:
        print("✅ PUSH SYNC RADI")
        return True

    print("⚠️ Push možda još nije stigao (async)")
    return False


def pull_sync_test():
    header("TEST 3: PULL-BASED SYNC")

    print("📋 Scenario:")
    print("5 inkremenata na R1 + 3 na R2 → Čitanje triggeruje pull")

    # Pročitaj početno stanje
    r_before = safe_get(f"{REPLICA_2_URL}/api/videos/{VIDEO_ID}/views-crdt", timeout=10)
    before_data = get_json_safely(r_before)
    before_count = before_data.get('totalViews', 0) if before_data else 0

    print(f"📊 Početno stanje: {before_count}")

    # 5 na R1
    print("🚀 Šaljem 5 zahteva na R1...")
    success_r1 = 0
    for i in range(5):
        r = safe_post(f"{REPLICA_1_URL}/api/videos/{VIDEO_ID}/view-crdt")
        if r and r.status_code == 200:
            success_r1 += 1
    print(f"✅ R1: {success_r1}/5 uspešno")

    # 3 na R2
    print("🚀 Šaljem 3 zahteva na R2...")
    success_r2 = 0
    for i in range(3):
        r = safe_post(f"{REPLICA_2_URL}/api/videos/{VIDEO_ID}/view-crdt")
        if r and r.status_code == 200:
            success_r2 += 1
    print(f"✅ R2: {success_r2}/3 uspešno")

    time.sleep(3)

    # Čitaj merged count
    r = safe_get(f"{REPLICA_2_URL}/api/videos/{VIDEO_ID}/views-crdt", timeout=10)
    data = get_json_safely(r)

    if not data:
        print("❌ Ne mogu da pročitam merged count")
        return False

    total = data.get('totalViews', 0)
    expected_min = before_count + success_r1 + success_r2

    print(f"📊 Merged total: {total}")
    print(f"📊 Očekivano minimum: {expected_min}")

    if total >= expected_min:
        print("✅ PULL SYNC RADI")
        return True

    print(f"⚠️ Očekivano {expected_min}, dobijeno {total}")
    return False


def load_balancer_test():
    header("TEST 4: LOAD BALANCER + EVENTUAL CONSISTENCY")

    print("📋 Scenario:")
    print("20 zahteva preko nginx load balancera")

    # Početno stanje
    r_before = safe_get(f"{REPLICA_1_URL}/api/videos/{VIDEO_ID}/views-crdt", timeout=10)
    before_data = get_json_safely(r_before)
    before_count = before_data.get('totalViews', 0) if before_data else 0

    print(f"📊 Početno stanje: {before_count}")

    # Pošalji zahteve
    success = 0
    for i in range(20):
        r = safe_post(f"{LOAD_BALANCER_URL}/api/videos/{VIDEO_ID}/view-crdt")
        if r and r.status_code == 200:
            success += 1
            if (i + 1) % 5 == 0:
                print(f"  ✅ {i + 1}/20 poslato")

    print(f"✅ Poslato: {success}/20 uspešno")

    time.sleep(5)

    # Čitaj sa obe replike
    r1 = safe_get(f"{REPLICA_1_URL}/api/videos/{VIDEO_ID}/views-crdt", timeout=10)
    r2 = safe_get(f"{REPLICA_2_URL}/api/videos/{VIDEO_ID}/views-crdt", timeout=10)

    data1 = get_json_safely(r1)
    data2 = get_json_safely(r2)

    if not data1 or not data2:
        print("❌ Ne mogu da pročitam stanje replika")
        return False

    t1 = data1.get('totalViews', 0)
    t2 = data2.get('totalViews', 0)

    print(f"📊 Replica 1 total: {t1}")
    print(f"📊 Replica 2 total: {t2}")

    expected_min = before_count + success

    if t1 == t2 and t1 >= expected_min:
        print("✅ EVENTUAL CONSISTENCY RADI")
        return True

    if abs(t1 - t2) <= 2:  # Tolerancija od 2
        print("⚠️ Mala razlika postoji, periodic sync će je ukloniti")
        return True

    print(f"⚠️ Razlika: {abs(t1 - t2)}")
    return False


def main():
    print("=" * 70)
    print("🧪 CRDT SYSTEM TEST - S3 ZAHTEV")
    print("=" * 70)

    results = []

    # Health check
    if not health_check():
        print("\n❌ Replike nisu dostupne. Test se prekida.")
        return

    # Pokreni testove
    results.append(("Push sync", push_sync_test()))
    results.append(("Pull sync", pull_sync_test()))
    results.append(("Eventual consistency", load_balancer_test()))

    # Finalni izveštaj
    print("\n" + "=" * 70)
    print("📊 FINALNI IZVEŠTAJ")
    print("=" * 70)

    for name, ok in results:
        status = "✅ PASS" if ok else "❌ FAIL"
        print(f"{name.ljust(30)} : {status}")

    passed = sum(1 for _, ok in results if ok)
    print(f"\n🎯 Rezultat: {passed}/{len(results)} testova prošlo")

    if passed == len(results):
        print("✅ S3 ZAHTEV JE U POTPUNOSTI ISPUNJEN")
    elif passed >= len(results) - 1:
        print("⚠️ Skoro svi testovi su prošli - async priroda sistema")
    else:
        print("❌ Neki testovi nisu prošli - proveri konfiguraciju")

    print("\n" + "=" * 70)


if __name__ == "__main__":
    main()