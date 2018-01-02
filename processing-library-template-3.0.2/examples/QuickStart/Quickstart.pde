/**
 * 
 * The PeasyCam3 Processing library.
 *
 *   Copyright 2008 Jonathan Feinberg
 *   Copyright 2018 Thomas Diewald
 *
 *   https://github.com/diwi/peasycam/tree/PeasyCam3
 *
 *   Apache License: http://www.apache.org/licenses/LICENSE-2.0
 * 
 * 
 * explanatory notes:
 * 
 * This library is a derivative of the original PeasyCam Library by Jonathan Feinberg 
 * and combines new useful features with the great look and feel of the original version.
 * 
 * It is designed to be in sync with "p5.EasyCam", the Javascript version for p5js.
 * 
 * 
 */



import peasycam3.PeasyCam;


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