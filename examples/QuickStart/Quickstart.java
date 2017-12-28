package QuickStart;

import peasycam3.PeasyCam;
import processing.core.PApplet;


public class Quickstart extends PApplet {
  
  //
  // simplest way to add a 3D camera to a sketch
  //
  
  public void settings() {
    size(800, 600, P3D);
  }
	
	public void setup() {
	  new PeasyCam(this);
	}

	public void draw() {
		rotateX(-.5f);
		rotateY(-.5f);
		lights();
		scale(10);
		strokeWeight(1/10f);
		background(0);
		fill(255, 0, 0);
		box(30);
		pushMatrix();
		translate(0, 0, 20);
		fill(0, 0, 255);
		box(5);
		popMatrix();
	}

  public static void main(String args[]) {
    PApplet.main(new String[] { Quickstart.class.getName() });
  }
  
}
