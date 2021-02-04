package finalproject.entities;

public class Person implements java.io.Serializable {

	private static final long serialVersionUID = 4190276780070819093L;

	// this is a person object that you will construct with data from the DB
	// table. The "sent" column is unnecessary. It's just a person with
	// a first name, last name, age, city, and ID.
	private int id;
	private String firstName;
	private String lastName;
	private int age;
	private String city;

	public Person(int id, String firstName, String lastName, int age, String city) {
		this.id = id;
		this.firstName = firstName;
		this.lastName = lastName;
		this.age = age;
		this.city = city;
	}
	public int getId() {
		return this.id;
	}
	public String getFirstName() {
		return this.firstName;
	}
	public String getLastName() {
		return this.lastName;
	}
	public int getAge() {
		return this.age;
	}
	public String getCity() {
		return this.city;
	}
}
