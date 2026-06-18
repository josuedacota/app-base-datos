import org.mindrot.jbcrypt.BCrypt;

public class GenerarHash {
    public static void main(String[] args) {
        String password = "admin123";
        String hash = BCrypt.hashpw(password, BCrypt.gensalt());
        System.out.println("HASH: " + hash);
        System.out.println("VERIFICACION: " + BCrypt.checkpw(password, hash));
    }
}