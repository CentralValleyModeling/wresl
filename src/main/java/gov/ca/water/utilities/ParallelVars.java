package gov.ca.water.utilities;

public class ParallelVars {
	public int dataDay;
	public int dataMonth;
	public int dataYear;
	public int timeArrayIndex;

	// Constructor 1
	public ParallelVars() {}

	// Constructor 2
	public ParallelVars(int day, int month, int year) {
	    this.dataDay = day;
		this.dataMonth = month;
		this.dataYear = year;
	}

	// Check if a date is earlier than this date
	public boolean isEarlierThan(ParallelVars compareDate) {
		if (compareDate.dataYear < this.dataYear) {
			// Compared year is less than this year
			return false;
		} else if (compareDate.dataYear > this.dataYear) {
			// Compared year is greater than this year
			return true;
		} else {
			// Compared year is equal to this year
			if (compareDate.dataMonth < this.dataMonth) {
				// Compared month is less than this month
				return false;
			} else if (compareDate.dataMonth > this.dataMonth) {
				// Compared month is greater than this month
				return true;
			} else {
				// Compared month is equal to this month
				if (compareDate.dataDay < this.dataDay) {
					// Compared day is less than this day
					return false;
				} else if (compareDate.dataDay > this.dataDay) {
					// Compared day is greater than this day
					return true;
				} else {
					// Compared day is equal to this day
					return false;
				}
			}
		}
	}
}
