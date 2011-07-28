/*
 * 	Reads in obj files, a fairly simplistic model format.
 * 
 * 
 */
package engine.importing;

import engine.render.Model;
import engine.render.model_pieces.*;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;

import javax.vecmath.Vector3f;

public class Obj_Parser extends Parser {

	private ArrayList<Vector3f> vertices = new ArrayList<Vector3f>(); // Vertex
																		// Coordinates
	private ArrayList<Vector3f> vertexNormals = new ArrayList<Vector3f>(); // Vertex
																			// Coordinates
																			// Normals
	// private ArrayList<Vector3f> vertexTextures = new ArrayList<Vector3f>();
	// // Vertex Coordinates Textures

	//private static int faceCount = 0;
	private Model model;

	public Obj_Parser() {
		super();
		model = new Model();
	}

	@Override
	public void readFile(String fileName) throws Exception {
		parseObj(
			new BufferedReader(
				new InputStreamReader(
					this.getClass().getClassLoader().getResourceAsStream(fileName)
				)
			)
		);
	}

	@Override
	public void readUrl(String url) throws Exception {
		BufferedReader file = new BufferedReader(
			new InputStreamReader(
				this.getClass().getClassLoader().getResourceAsStream(url)
			)
		);
		parseObj(file);
	}

	public void parseObj(BufferedReader file) throws Exception {
		Mesh mesh = new Mesh();
		String currentLine = file.readLine();

		while (!currentLine.subSequence(0, 2).equals("v ")) {
			// System.out.println("***" + currentLine.subSequence(0, 2) +
			// "***");
			currentLine = file.readLine();
		}

		// Read Vertices
		while (currentLine.subSequence(0, 2).equals("v ")) {
			vertices.add(parseVector(currentLine));
			currentLine = file.readLine();
		}

		// Skip other shit
		while (!currentLine.subSequence(0, 3).equals("vn ")) {
			currentLine = file.readLine();
		}

		// Read Normals
		while (currentLine.subSequence(0, 3).equals("vn ")) {
			vertexNormals.add(parseVector(currentLine));
			currentLine = file.readLine();
		}

		// Skip other shit
		while (!currentLine.subSequence(0, 2).equals("f ")) {
			currentLine = file.readLine();
		}

		// Read faces
		while (currentLine != null && currentLine.subSequence(0, 2).equals("f ")) {
			mesh.addFace(readFace(currentLine));
			currentLine = file.readLine();
			if (currentLine == null) {
				break;
			}
		}

		// Give mesh fake material
		mesh.setMaterial(new Material());
		model.addMesh(mesh);
	}

	private Face readFace(String string) throws Exception {
		Face f = new Face();
		String[] verts;

		// Split on white Spaces
		String[] tokens = string.trim().split("\\s+");

		for (int i = 1; i < tokens.length; i++) {
			verts = tokens[i].split("/");

			if (verts.length == 3) {
				f.addVertex(vertices.get(Integer.parseInt(verts[0]) - 1));
				f.addVertexNorm(vertexNormals.get(Integer.parseInt(verts[2]) - 1));
			} else {
				System.out.println("Weird OBJ file.  Uses alternative face format");
				throw new Exception();
			}
		}
		//faceCount++;
		return f;
	}

	private Vector3f parseVector(String string) throws Exception {
		String[] line = string.trim().split("\\s+");

		return new Vector3f(
			Float.parseFloat(line[1]), Float.parseFloat(line[2]), Float.parseFloat(line[3])
		);
	}

	@Override
	public Model createModel() {
		return new Model(model);
	}

}