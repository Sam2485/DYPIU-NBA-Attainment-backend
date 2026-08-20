const bcrypt = require('bcryptjs');

const hash_pc1 = "$2a$10$ugkNHmc/PfzOAfGp2RNT1OJeUpec17R0Cs9N6Wh3y5EpmMRmW8.Kq";
const hash_pc2 = "$2a$10$YrqSz/lNYqTfBjZZ179qUuO1lW5FCImha.PZ8QyIqGFCjSL7fEHjG";
const passwords = ["password", "password123", "admin", "admin123", "pc1", "pc1@123", "pc1@gmail.com", "Test@123", "123456", "12345678", "dypiu", "dypiu@123", "Prasad@123", "prasad"];

passwords.forEach(p => {
    if (bcrypt.compareSync(p, hash_pc1)) console.log(`Found PC1 password: ${p}`);
    if (bcrypt.compareSync(p, hash_pc2)) console.log(`Found PC2 password: ${p}`);
});
