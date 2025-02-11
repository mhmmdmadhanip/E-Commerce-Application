package com.app;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.IntStream;

import org.apache.commons.lang3.StringUtils;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

import com.app.config.AppConstants;
import com.app.entites.*;
import com.app.repositories.*;
import com.github.javafaker.Faker;

import io.swagger.v3.oas.annotations.enums.SecuritySchemeIn;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.security.SecurityScheme;

@SpringBootApplication
@SecurityScheme(name = "E-Commerce Application", scheme = "bearer", type = SecuritySchemeType.HTTP, in = SecuritySchemeIn.HEADER)
public class ECommerceApplication implements CommandLineRunner {

    @Autowired
    private UserRepo userRepo;
    @Autowired
    private RoleRepo roleRepo;
    @Autowired
    private AddressRepo addressRepo;
    @Autowired
    private CategoryRepo categoryRepo;
    @Autowired
    private ProductRepo productRepo;
    @Autowired
    private CartRepo cartRepo;
    @Autowired
    private OrderRepo orderRepo;
    @Autowired
    private PaymentRepo paymentRepo;
    @Autowired
    private CartItemRepo cartItemRepo;
    @Autowired
    private OrderItemRepo orderItemRepo;
    @Autowired
	private PasswordEncoder passwordEncoder;


    private final Faker faker = new Faker(new Locale("in-ID"));

    public static void main(String[] args) {
        SpringApplication.run(ECommerceApplication.class, args);
    }

    @Bean
    public ModelMapper modelMapper() {
        return new ModelMapper();
    }

    @Override
    @Transactional
    public void run(String... args) {
        try {
            System.out.println("Starting database seeding...");
            seedData();
            System.out.println("Database seeding completed successfully!");
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Database seeding failed!");
        }
    }

    @Transactional
    public void seedData() {
        seedRoles();
        seedAddresses();
        seedUsers();
        seedCategories();
        seedProducts();
        seedCarts();
        seedOrders();
        seedPayments();
        seedCartItems();
        seedOrderItems();
    }

    private void seedRoles() {
        if (roleRepo.count() == 0) {
            Role adminRole = new Role(AppConstants.ADMIN_ID, "ADMIN");
            Role userRole = new Role(AppConstants.USER_ID, "USER");
            roleRepo.saveAll(List.of(adminRole, userRole));
            System.out.println("Roles seeded.");
        }
    }

    private void seedAddresses() {
        if (addressRepo.count() == 0) {
            List<Address> addresses = IntStream.range(0, 10)
                .mapToObj(i -> new Address(null, faker.address().streetName(), faker.company().name(),
                        faker.address().city(), faker.address().state(), faker.address().country(),
                        StringUtils.rightPad(faker.address().zipCode(), 6, '0'), new ArrayList<>()))
                .toList();
            addressRepo.saveAll(addresses);
            System.out.println("Addresses seeded.");
        }
    }

    private void seedUsers() {
        if (userRepo.count() == 0) {
            List<Role> roles = roleRepo.findAll();
            List<Address> addresses = addressRepo.findAll();

            List<User> users = IntStream.range(0, 10).mapToObj(i -> {
                User user = new User();
                user.setFirstName(generateValidName());
                user.setLastName(generateValidName());
                user.setMobileNumber(faker.numerify("##########"));
                user.setEmail(faker.internet().safeEmailAddress());
                String encodedPass = passwordEncoder.encode("password");
                user.setPassword(encodedPass);

                if (!roles.isEmpty()) {
                    user.setRoles(new HashSet<>(Collections.singletonList(roles.get(0))));
                }

                if (!addresses.isEmpty()) {
                    user.setAddresses(Collections.singletonList(addresses.get(i % addresses.size())));
                }

                return user;
            }).toList();

            userRepo.saveAll(users);
            System.out.println("Users seeded.");
        }
    }

    private void seedCategories() {
        if (categoryRepo.count() == 0) {
            List<Category> categories = IntStream.range(0, 10)
                .mapToObj(i -> new Category(null, generateValidCategoryName(), new ArrayList<>()))
                .toList();
            categoryRepo.saveAll(categories);
            System.out.println("Categories seeded.");
        }
    }

    private void seedProducts() {
        if (productRepo.count() == 0) {
            List<Category> categories = categoryRepo.findAll();
            List<Product> products = IntStream.range(0, 10)
                .mapToObj(i -> new Product(null, faker.commerce().productName(), faker.internet().image(),
                        faker.lorem().sentence(), faker.number().numberBetween(10, 100),
                        faker.number().randomDouble(2, 10, 500), faker.number().randomDouble(2, 0, 50),
                        faker.number().randomDouble(2, 5, 450), categories.get(i % categories.size()),
                        new ArrayList<>(), new ArrayList<>()))
                .toList();
            productRepo.saveAll(products);
            System.out.println("Products seeded.");
        }
    }

    private void seedCarts() {
        if (cartRepo.count() == 0) {
            List<User> users = userRepo.findAll();
            List<Cart> carts = users.stream()
                .map(user -> new Cart(null, user, new ArrayList<>(), faker.number().randomDouble(2, 10, 500)))
                .toList();
            cartRepo.saveAll(carts);
            System.out.println("Carts seeded.");
        }
    }

    private void seedOrders() {
        if (orderRepo.count() == 0) {
            List<User> users = userRepo.findAll();
            List<Order> orders = users.stream()
                .map(user -> new Order(null, user.getEmail(), new ArrayList<>(), LocalDate.now(), null,
                        faker.number().randomDouble(2, 50, 1000), "Pending"))
                .toList();
            orderRepo.saveAll(orders);
            System.out.println("Orders seeded.");
        }
    }

    private void seedPayments() {
        if (paymentRepo.count() == 0) {
            List<Order> orders = orderRepo.findAll();
            List<Payment> payments = orders.stream()
                .map(order -> new Payment(null, order, faker.finance().creditCard()))
                .toList();
            paymentRepo.saveAll(payments);
            System.out.println("Payments seeded.");
        }
    }

    private void seedCartItems() {
        if (cartItemRepo.count() == 0) {
            List<Cart> carts = cartRepo.findAll();
            List<Product> products = productRepo.findAll();
            List<CartItem> cartItems = carts.stream()
                .flatMap(cart -> IntStream.range(0, 3)
                    .mapToObj(i -> new CartItem(null, cart, products.get(i % products.size()), 
                            faker.number().numberBetween(1, 5), faker.number().randomDouble(2, 0, 20),
                            faker.number().randomDouble(2, 5, 450))))
                .toList();
            cartItemRepo.saveAll(cartItems);
            System.out.println("Cart items seeded.");
        }
    }

    private void seedOrderItems() {
        if (orderItemRepo.count() == 0) {
            System.out.println("Order items seeded.");
        }
    }

    private String generateValidName() {
        String name;
        do {
            name = faker.name().firstName();
        } while (name.length() < 5 || name.length() > 20);
        return name;
    }

    private String generateValidCategoryName() {
        String name;
        do {
            name = faker.commerce().department();
        } while (name.length() < 5);
        return name;
    }
}