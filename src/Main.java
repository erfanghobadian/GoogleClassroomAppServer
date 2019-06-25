import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;



class User implements Serializable {
    static ArrayList <User> users;
    String username ;
    String password ;
    User(String username , String password) {
        this.username = username;
        this.password = password;
        users.add(this) ;
        save();
    }



    // Read Users From File
    static {
        try (ObjectInputStream inputStream = new ObjectInputStream(new FileInputStream("users.ser"))) {
            users = (ArrayList<User>) inputStream.readObject();
        } catch (IOException | ClassNotFoundException e) {
            users = new ArrayList<>();
        }
    }

    // Save Users in File
    static private void save() {
        try (ObjectOutputStream outputStream = new ObjectOutputStream(new FileOutputStream("users.ser"))) {
            outputStream.writeObject(users);
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

}


class ClientHandler extends Thread{
    static ArrayList<ClientHandler>  Clients = new ArrayList<>();
    Socket s ;
    ObjectOutputStream oos ;
    ObjectInputStream ois;
    DataOutputStream dos ;


    // Find The User
    static private User findUser(String username, String password) {
        for (User user:User.users) {
            if (user.username.equals(username) && user.password.equals(password))
                return user ;
        }
        return null ;
    }

    // Check if Username Exists
    static private Boolean UserNameCheck(String username) {
        for(User user:User.users) {
            if (user.username.equals(username))
                return false;
        }
        return true;
    }

    ClientHandler(Socket cs) {
        this.s = cs ;
    }
    @Override
    public void run() {
        try {
            ois = new ObjectInputStream(s.getInputStream());
            oos = new ObjectOutputStream(s.getOutputStream());
            String[] a = (String[])ois.readObject();
            System.out.println(a[0]);
            oos.flush();

            if(a[0].equals("Login")) {
                System.out.println("Check For Login");
                String username = a[1];
                String password = a[2];
                User user = findUser(username ,password) ;
                if (user !=null) {
                    System.out.println("Found");
                    oos.writeBoolean(true);
                    oos.flush();
                }
                else {
                    System.out.println("Wrong U & P");
                    oos.writeBoolean(false);
                    oos.flush();

                }

            }
            else if (a[0].equals("Register")) {
                System.out.println("Request For Register");
                String username = a[1];
                String password = a[2];
                User newUser = new User(username,password);
                oos.writeBoolean(true);
                oos.flush();

            }
            else if (a[0].equals("UserNameCheck")) {
                System.out.println("Check For Username");
                String username = a[1];
                oos.writeBoolean(UserNameCheck(username));
                oos.flush();
            }



            oos.close();
            ois.close();
            s.close();

        }catch (Exception e){
            e.printStackTrace();
        }
        System.out.println("Client Exited");
    }
}

public class Main {

    public static void main(String[] args) throws Exception {
        ServerSocket ss = new ServerSocket(8080) ;

        while (true) {
            Socket cs =  ss.accept();
            System.out.println("Client Connected");
            ClientHandler newClient =  new ClientHandler(cs);
            ClientHandler.Clients.add(newClient);
            newClient.start();

        }
    }
}
