import java.util.LinkedList;
import java.util.Queue;
import java.util.Random;
import java.util.concurrent.Semaphore;
public class PostOffice {
	
	
	public static Semaphore maxCapacity = new Semaphore(10,true);	//Semaphore to allow only 10 into the post office at a time
	public static Semaphore ready = new Semaphore(0,true);			//Semaphore to indicate when a customer is ready to 
	public static Semaphore workers = new Semaphore(3, true);		//Semaphore to allow only three customers through at a time for workers
	public static Semaphore scale = new Semaphore(1,true);			//Semaphore to only allow one use of scale at a time
    public static Semaphore mutex = new Semaphore(1,true);			//Semaphore mutex for queue up one Customer at a time
    public static Semaphore gate1 = new Semaphore(0,true);			//Semaphore to ensure one customer at a time unloading from queue
    public static Semaphore gate2 = new Semaphore(0,true);			//Semaphore used to help dequeuing Customers to accomplish role in worker class
    public static Semaphore leaveDesk = new Semaphore(0,true);		//Semaphore to direct ensure Worker is not release until customer has left store.
    
    
    public static Semaphore finished[] = new Semaphore[50];
    
    public static Queue<Integer> queue = new LinkedList<>();		//Queue to place
	
    
    public static String roleMessage;								//String to communicate what role the customer is wanting
	public static String roleMessage2;								//String to communicate what role was completed
	
	static {
        for (int i = 0; i < 50; i++) {
            finished[i] = new Semaphore(0, true);
        }
    }
	
	public static Customer[] objCust = new Customer[50];
    public static Worker[] objWork = new Worker[3];
    
	
	
    
    
    //This is the Customer Class
	public static class Customer implements Runnable
	{
	   public int num;
	   public int role;
	   public int assignedWorker;
	   
	   //Customer Constructor
	   Customer(int num) 
	   {
	      this.num = num;
	   }
	
	public void run() {
		
		System.out.println( "Customer " + num + " created" );
		
		try { 
		assign();
		
		maxCapacity.acquire();			
		enter();
		workers.acquire();
		mutex.acquire();
		enqueue(num);
		ready.release();
		mutex.release();				
		gate1.acquire();
		asks();
		gate2.release();
		finished[num].acquire();
		finished();
		leave();
		leaveDesk.release();
		maxCapacity.release();
		}
		catch (InterruptedException e) {

        }
	   }
	
	//Assign a role for customer when created (1-3)
	private void assign() {
		Random r = new Random();
        int low = 1;
        int high = 4;
        this.role = r.nextInt(high - low) + low;
        
	}
	
	//Text to display what customer is entering the shop
	public void enter() {
		System.out.println("Customer " + num + " enters post office");
	}
	
	public void leave() {
		System.out.println("Customer " + num + " left the post office");
	}
	
	public void finished() {
		System.out.println("Customer " + num + " finished" + roleMessage2(role));
	}
	
	public void asks() {
		
		System.out.println("Customer " + num + " asks Postal worker " + assignedWorker + " to" + roleMessage(role));
		objWork[assignedWorker].workerQueue.add(role);
		
		
	}
	
	public String roleMessage(int x) {
		if(x == 1) {
			roleMessage = " buy stamps ";
		}
		if(x == 2) {
			roleMessage = " mail a letter ";
		}
		if(x==3) {
			roleMessage = " mail a package ";
		}
		return roleMessage;
	}
	
	public String roleMessage2(int x) {
		if(x == 1) {
			roleMessage2 = " buying stamps ";
		}
		if(x == 2) {
			roleMessage2 = " mailing a letter ";
		}
		if(x==3) {
			roleMessage2 = " mailing a package ";
		}
		return roleMessage2;
	}
	
	//Method to queue a Customer into the 
	public static void enqueue(int x) {
		
		queue.add(x);
		
	}
	
}
	
	
	
	//Postal Worker class
	public static class Worker implements Runnable
	{
	   public int number;
	   private static int customer;
	   public Queue<Integer> workerQueue  = new LinkedList<>();
	   
	   //Postal Worker Constructor
	   Worker(int number) 
	   {
	      this.number = number;
	   }
	
	public void run() {
	     
		System.out.println( "Postal Worker " + number + " created" );
		
		while (true) {
			try {
			
			ready.acquire();
	        mutex.acquire();
	        dequeue();
	        mutex.release();
	        gate1.release();
	        gate2.acquire();
	        serve();
	        finished[customer].release();
	        leaveDesk.acquire();
	        workers.release();
			}
			catch (InterruptedException e) {

	        }
	    }
	  
	  }
	
	//Dequeue method to remove the assigned role for the next customer and place in worker queue
	private void dequeue() {
		customer = queue.remove();
		System.out.println("Postal worker " + number + " serving customer " + customer);
		objCust[customer].assignedWorker = number;
		
	}
	
	//Method to serve customers based on what role they have, and do the task accordingly
	public void serve() {
		int job = workerQueue.remove();
		if(job == 1) {
			try {
				Thread.sleep(60000);
			} catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
            }
			System.out.println("Postal worker " + number + " finished selling stamps for Customer " + customer);
		}
		if(job == 2) {
			try {
				Thread.sleep(90000);
			} catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
            }
			System.out.println("Postal worker " + number + " finished mailing a letter for Customer " + customer);
		}
		if(job == 3) {
			try {
				scale.acquire();
				System.out.println("Scales in use by Postal worker " + number);
				Thread.sleep(120000);
			} catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
            }
			System.out.println("Scales released by Postal worker " + number);
			scale.release();
			System.out.println("Postal worker " + number + " finished mailing a package for Customer " + customer);
		}
	}
	
	
	
}
	
	
	
	
	
	
	//Driver program
	public static void main(String args[]) {
		     
		//Create Worker Threads
		final int totalWorkers = 3;
		
		for(int z = 0; z < totalWorkers; z++ ) {
			
		    Thread[] worker = new Thread[3];
			objWork[z] = new Worker(z);
			worker[z] = new Thread( objWork[z] );
		    worker[z].start();
		}
		
		//Create Customer Threads
		final int totalCustomers = 50;
		Thread[] customer = new Thread[50];
	      // create threads
	    for(int i = 0; i < totalCustomers; i++ ) {
		 objCust[i] = new Customer(i);    
		 customer[i] = new Thread( objCust[i] );
	     customer[i].start();
	      
	     }
	      
		 //Join the Customer threads
	      for (int y = 0; y < totalCustomers; y++) {
	            
	    	  try {
	            	
	    		 customer[y].join();
	              System.out.println("Joined customer " + y);
	            } 
	    	  catch (InterruptedException e) {

	            }
	        }
	}
	
}
