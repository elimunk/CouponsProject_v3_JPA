package com.elimunk.coupons.logic;

import java.util.List;

import javax.transaction.Transactional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

import com.elimunk.coupons.beans.Customer;
import com.elimunk.coupons.beans.PostLoginUserData;
import com.elimunk.coupons.dao.ICustomersDao;
import com.elimunk.coupons.enums.ErrorTypes;
import com.elimunk.coupons.exceptions.ApplicationException;
import com.elimunk.coupons.utils.DateUtils;
import com.elimunk.coupons.utils.ValidateUtils;

@Controller
public class CustomersController {

	@Autowired
	private ICustomersDao customerDao;
	@Autowired
	private UsersController usersController;

	public CustomersController() {
	}

		
	@Transactional
	public void createCustomer(Customer customer) throws ApplicationException {
		// create (and validate) the user of customer by 'usersController' level 
		long customerId = usersController.createUser(customer.getUserCustomer());
		customer.setId(customerId);
		// validate the customer before the creating 
		validateCustomer(customer);
		validateEmailNotExist(customer);
		validatePhoneNotExist(customer);
		// if the customer is valid - add to the database.
		customerDao.save(customer);
	}
	
	public void updateCustomer(Customer customer, PostLoginUserData userData) throws ApplicationException {
		validateUserAccess(customer.getId(), userData);
		// Check if the customer exists. before the action
		validateExistCustomer(customer.getId());
		// Check if the customer changes is valid for update. before the action
		validateCustomer(customer);
		// if customer change the email, validate that the email not already taken
		if (!customerDao.findById(customer.getId()).get().getEmail().equals(customer.getEmail())) {
			validateEmailNotExist(customer);
		}
		// if customer change the Phone, validate that the Phone not already taken
		if (!customerDao.findById(customer.getId()).get().getPhoneNumber().equals(customer.getPhoneNumber())) {
			validatePhoneNotExist(customer);
		}
		// Update the customer in the database
		customerDao.save(customer);
	}
	
	@Transactional
	public void deleteCustomer(long customerId, PostLoginUserData userData) throws ApplicationException {
		validateUserAccess(customerId, userData);
		// Check if the customer exists. before the action
		validateExistCustomer(customerId);
		// delete the customer (the user details and purchases, coupons, and users deleted by cascade)
		customerDao.deleteById(customerId);
	}

	public Customer getCustomer(long customerId) throws ApplicationException {
		return customerDao.findById(customerId).get();
	}
	
	public Customer getCustomerDetails(long costomerId) throws Exception {
		return customerDao.findById(costomerId).get();
	}
	
	public List<Customer> getAllCustomers() throws ApplicationException {
		return (List<Customer>) customerDao.findAll();
	}
	
	// validate the customer before adding or updating .
	private void validateCustomer(Customer customer) throws ApplicationException {
		// Because this validate is for create OR update - we check if the user is null or not
		// When we Create customer, user couldn't be a null because is first created . for update customer -the user not need to be sent again, and could be a null
		if (customer.getUserCustomer() != null) {
			// the user type must be a customer type
			if (!customer.getUserCustomer().getType().name().equals("CUSTOMER")) {
				throw new ApplicationException(ErrorTypes.INVALID_USER_TYPE, DateUtils.getCurrentDateAndTime(),
						"Create/update customer Failed. The user is not a customer user. " ,false);
			}
		}
		// the customer first name and last name must contains letters only and must
		// contains 2-50 characters
		if (!ValidateUtils.isNameValid(customer.getFirstName()) || !ValidateUtils.isNameValid(customer.getLastName())) {
			throw new ApplicationException(ErrorTypes.INVALID_NAME, DateUtils.getCurrentDateAndTime(),
					"Create/update customer Failed. Name '" + customer.getFirstName() + " " + customer.getLastName() + "' is not valid! only letters are allowed , and must contains 2-50 characters",false);
		}
		// the customer email must be a regular expression. Example: 'email@email.com'.
		if (!ValidateUtils.isEmailValid(customer.getEmail())) {
			throw new ApplicationException(ErrorTypes.INVALID_EMAIL, DateUtils.getCurrentDateAndTime(),
					"Email '" + customer.getEmail() + "'  is not valid! email must be a regular expression. Example: 'email@email.com'. ",false);
		}
		// the customer phone number must contains 10 digits only and start with '05'.
		if (!ValidateUtils.isPhoneNumberValid(customer.getPhoneNumber())) {
			throw new ApplicationException(ErrorTypes.INVALID_PHONE_NUMBER, DateUtils.getCurrentDateAndTime(),
					"Phone number '" + customer.getPhoneNumber() + "' is not valid! must contains 10 digits only and start with '05'. ",false);
		}
	}

	private void validateEmailNotExist(Customer customer) throws ApplicationException {
		// the customer email cannot be taken
		if (customerDao.existsByEmail(customer.getEmail())) {
			throw new ApplicationException(ErrorTypes.EMAIL_EXIST, DateUtils.getCurrentDateAndTime(), "The Email address already taken. ",false);
		}
	}
	private void validatePhoneNotExist(Customer customer) throws ApplicationException {
		// the customer email cannot be taken
		if (customerDao.existsByPhoneNumber(customer.getPhoneNumber())) {
			throw new ApplicationException(ErrorTypes.PHONE_EXIST, DateUtils.getCurrentDateAndTime(), "The Phone number already taken. ",false);
		}
	}

	// validate that the customer exist
	private void validateExistCustomer(long customerId) throws ApplicationException {
		if (!customerDao.existsById(customerId)) {
			throw new ApplicationException(ErrorTypes.NOT_EXIST, DateUtils.getCurrentDateAndTime(), "This customer does not exist.",false);
		}
	}
	
	// checks that the user have the correct user Id before operation on the customer
	private void validateUserAccess(long customerId, PostLoginUserData userData) throws ApplicationException {
		// if the user type is not administrator, check that the customer id is correct
		if (userData.getClientType().name() != "ADMINISTRATOR") {
			if (customerId != userData.getUserId()) {
				// if the user customer Id is not correct throw HACKING ERROR
				throw new ApplicationException(ErrorTypes.HACKING_ERROR, DateUtils.getCurrentDateAndTime(),
						" HACKING! customer id is not correct!" , true);
			}
		}
	}

}
