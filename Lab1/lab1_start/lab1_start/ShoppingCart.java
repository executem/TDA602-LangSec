import backEnd.*;
import java.util.Scanner;

public class ShoppingCart {
    private static void print(Wallet wallet, Pocket pocket) throws Exception {
        System.out.println("Your current balance is: " + wallet.getBalance() + " credits.");
        System.out.println(Store.asString());
        System.out.println("Your current pocket is:\n" + pocket.getPocket());
    }

    private static String scan(Scanner scanner) throws Exception {
        System.out.print("What do you want to buy? (type quit to stop) ");
        return scanner.nextLine();
    }

    public static void main(String[] args) throws Exception {
        Wallet wallet = new Wallet();
        Pocket pocket = new Pocket();
        Scanner scanner = new Scanner(System.in);

        print(wallet, pocket);
        String product = scan(scanner);
        
        while(!product.equals("quit")) {
            try {
                Integer price = Store.getProductPrice(product);
                if(wallet.getBalance() < price){
                    System.out.println("Balance not sufficient to buy product: " + product);
                    break;

                } else {
                    wallet.setBalance(wallet.getBalance() - price);
                    pocket.addProduct(product);
                    System.out.println("You bought " + product + " for " + price + " credits.");
                }
            } catch (IllegalArgumentException e) {
                System.out.println("Product " + product + " is not in store");
            }
            /* TODO:
               - check if the amount of credits is enough, if not stop the execution.
               - otherwise, withdraw the price of the product from the wallet.
               - add the name of the product to the pocket file.
               - print the new balance.
            */
           

            // Just to print everything again...
            print(wallet, pocket);
            product = scan(scanner);
        }
    }
}
