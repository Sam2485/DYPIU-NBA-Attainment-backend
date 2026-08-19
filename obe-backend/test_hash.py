import bcrypt

hash_pc1 = b"$2a$10$ugkNHmc/PfzOAfGp2RNT1OJeUpec17R0Cs9N6Wh3y5EpmMRmW8.Kq"
passwords = ["password", "password123", "admin", "admin123", "pc1", "pc1@123", "pc1@gmail.com", "Test@123", "123456", "12345678", "dypiu", "dypiu@123", "Prasad@123", "prasad"]

for p in passwords:
    if bcrypt.checkpw(p.encode('utf-8'), hash_pc1):
        print(f"Found PC1 password: {p}")
        break
else:
    print("PC1 Password not found in list.")
