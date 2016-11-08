import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;

public class SampleProjClass1Test 
{
	SampleProjClass1 testObject;
	
	@Before
	public void setup()
	{
		testObject = new SampleProjClass1();
	}
	
	@Test
	public void test1() 
	{
		assertEquals("Test1", 1, testObject.returnOne());
	}

}
