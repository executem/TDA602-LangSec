package backEnd;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;

public class Wallet {
    /**
     * The RandomAccessFile of the wallet file
     */  
    private RandomAccessFile file;

    /**
     * Creates a Wallet object
     *
     * A Wallet object interfaces with the wallet RandomAccessFile
     */
    public Wallet () throws Exception {
	this.file = new RandomAccessFile(new File("backEnd/wallet.txt"), "rw");
    }

    /**
     * Gets the wallet balance. 
     *
     * @return                   The content of the wallet file as an integer
     */
    public int getBalance() throws IOException {
	this.file.seek(0);
	return Integer.parseInt(this.file.readLine());
    }

    /**
     * Sets a new balance in the wallet
     *
     * @param  newBalance          new balance to write in the wallet
     */
    public void setBalance(int newBalance) throws Exception {
	this.file.setLength(0);
	String str = Integer.valueOf(newBalance).toString()+'\n'; 
	this.file.writeBytes(str); 
    }

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
        
    }

    /**
     * Closes the RandomAccessFile in this.file
     */
    public void close() throws Exception {
	this.file.close();
    }
}
