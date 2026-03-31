# Report

## Part0
To start compile the program:
```sh
make clean
make all
java shoppingCart
```

We implemented this part: 
```java
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
    ...}
```

## Part1 - Exploit your program
### What is the shared resource? Who is sharing it?
The wallet.txt file is the shared resource we will be attacking. We are sharing it with other instances of the program.

### What is the root of the problem?
That there is a time between reading our balance and deducting the price of the product from out wallet.
Also that the wallet is a shared resource, allowing it to be altered by different instances concurrently.

### How to attack the system
We believe that it's possible to attack the system through a timing attack. Using two instances of the program, they share the same wallet and pocket. The attack would go as following:
- One instance pause execution after the balance check on row 32
- The other instance completes a purchase of a car.
- The first instance resumes execution and completes the pruchase of a car.

This would result in pocket containing two cars and 0 balance (assuming that the wallet initially contains the price of a car).

To do this, we use a debugger to halt the execution and step over the call to `getBalance()`, in the first instance. Start another and proceed according to the steps above.

### Terminal 1
```
Your current balance is: 30000 credits.
car     30000
book    100
pen     40
candies 1

Your current pocket is:

What do you want to buy? (type quit to stop) car
You bought car for 30000 credits.
Your current balance is: 0 credits.
car     30000
book    100
pen     40
candies 1

Your current pocket is:
car
car

What do you want to buy? (type quit to stop) 
```
### Terminal 2
``` 
Your current balance is: 30000 credits.
car     30000
book    100
pen     40
candies 1

Your current pocket is:

What do you want to buy? (type quit to stop) car
You bought car for 30000 credits.
Your current balance is: 0 credits.
car     30000
book    100
pen     40
candies 1

Your current pocket is:
car

What do you want to buy? (type quit to stop) 
```
### Explanation
As we can see in the output from the first program, this execution results in two cars in the pocket and a balance of 0, which if the program executed as intended, should have resulted in an error and exit.

## Part2 - Fix the API
### Code for `safeWithdraw`
To implement `safeWithdraw()` we did:
```java
public boolean safeWithdraw(int valueToWithdraw) throws Exception {
    // Create a FileChannel from the RandomAccessFile 
    FileChannel channel = this.file.getChannel();
    // Create a lock and lock the file. The lock is not enforced unless checking it so getBalance() is still callable even if the lock is acquired.
    FileLock lock = channel.lock();
    
        int balance = this.getBalance();
        if (balance < valueToWithdraw){
            lock.release();
            return false;
        }
        else {
            this.setBalance(balance - valueToWithdraw);
            lock.release();
            return true;

    
}
```
This implementation locks wallet.txt on withdraw, meaning that any other instance also attempting to withdraw will have to wait for their turn. This stops the exploit since the case where both instances read the balance before any of them deduct from it can't happen.

### Is our implementation enough and not excessive?
Locking the channel is very ligthweight, it is not computationally excessive. We do lock the entire file, which would be excessive if we would want other instances to be able to access different parts of the file concurrently. 

### Other API data races
The shared resources are pocket and wallet, while the store is constant. The shared resources indicate the possibility of a data race due to non atomic calls to the get/set methods for these resources. 

We can for example get a data race in `print()`. If one instance purchases something using `safeWithdraw()`, but another instance prints the balance before it calls `addProduct()`, it may seem like balance have been drawn but nothing was purchased for the second instance. However as this is very unlikely to happen and more of a minor inconvinience it would be excessive to implement, for example, a universal lock to prevent this race.

There is a potential risk for data race when using `addProduct()`, `this.file.seek()` sets the pointer to where the next byte should be written in the file. If two instances purchases concurrently, they risk of writing to the same place as seek simply finds the position of the last byte. This can be mitigated by wrapping the `addProduct()` in a file lock as we have done below.

```java
public void safeAddProduct(String product) throws Exception {
    // Create a file lock to limit access to the pocket file to one instance at a time.
    FileChannel channel = this.file.getChannel();
    try (FileLock lock = channel.lock();) {
        this.file.seek(this.file.length());
        this.file.writeBytes(product+'\n');
    } catch (Exception e) {
        System.out.println(e.getStackTrace());
    } 
}
```