public class SampleProjClass1 
{

	public int returnOne()
	{
		int a = 0;
		a++;
		
		int b = 0;
		//b++;
		
		return a;
	}
	
	public static void main(String[] args)
	{
		SampleProjClass1 a = new SampleProjClass1();
		a.returnOne(); 
	}
}

