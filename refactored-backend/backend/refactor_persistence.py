import os
import shutil

src_persistence = "/home/oliad/Documents/internship_insa/ITAS_PRO/BUC3&5/bs-filing-core-server_1/src/main/java/com/act/filing/persistence"
dst_persistence = "/home/oliad/Documents/internship_insa/ITAS_PRO/BUC3&5/backend/src/main/java/com/itas/taxfiling/persistence"

src_db = "/home/oliad/Documents/internship_insa/ITAS_PRO/BUC3&5/bs-filing-core-server_1/src/main/resources/db/migration"
dst_db = "/home/oliad/Documents/internship_insa/ITAS_PRO/BUC3&5/backend/src/main/resources/db/migration"

def copy_and_refactor(src, dst):
    for item in os.listdir(src):
        s = os.path.join(src, item)
        d = os.path.join(dst, item)
        
        if os.path.isdir(s):
            os.makedirs(d, exist_ok=True)
            copy_and_refactor(s, d)
        else:
            if s.endswith(".java"):
                with open(s, "r") as f:
                    content = f.read()
                
                # Refactor packages
                content = content.replace("com.act.filing", "com.itas.taxfiling")
                
                # Since we renamed Get*UseCase to Query*UseCase, we might need to update imports in the persistence layer if they reference use cases (though adapters shouldn't import use cases, just in case)
                content = content.replace("GetTaxReturnUseCase", "QueryTaxReturnUseCase")
                
                with open(d, "w") as f:
                    f.write(content)

os.makedirs(dst_persistence, exist_ok=True)
copy_and_refactor(src_persistence, dst_persistence)

# Copy Flyway migrations
if os.path.exists(dst_db):
    shutil.rmtree(dst_db)
shutil.copytree(src_db, dst_db)

print("Persistence layer and Flyway migrations copied and refactored successfully.")
