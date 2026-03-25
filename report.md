# Report

# Part0
To start compile the program:
```
    make clean
    make all
    java shoppingCart
```

# Part1
## What is the shared resource? Who is sharing it?
The wallet.txt file is the shared resource we will be attacking. We are sharing it with other instances of the program.

## What is the root of the problem?
That there is a time between reading our balance and deducting the price of the product from out wallet.
Also that the wallet is a shared resource, allowing it to be altered by different instances concurrently.

## How to attack the system
We believe that it's possible to attack the system through a timing attack. Using two instances of the program, they share the same wallet and pocket. The attack would go as following:
- One instance pause execution after the balance check on row 32
- The other instance completes a purchase of a car.
- The first instance resumes execution and completes the pruchase of a car.

This would result in pocket containing two cars and 0 balance (assuming that the wallet initially contains the price of a car).

