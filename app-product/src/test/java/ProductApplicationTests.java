import com.whatap.common.event.EventItem;
import com.whatap.product.ProductApplication;
import com.whatap.product.dto.UpdateProductRequestDto;
import com.whatap.product.entity.Product;
import com.whatap.product.repository.ProductRepository;
import com.whatap.product.service.ProductLockService;
import com.whatap.product.service.ProductService;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


@Slf4j
@SpringBootTest(classes = ProductApplication.class)
public class ProductApplicationTests {

  @Autowired
  private ProductService productService;

  @Autowired
  private ProductLockService lockService;

  @Autowired
  private ProductRepository repository;

//  @SpyBean
//  private DistributedLockAop distributedLockAop;
//  @InjectMocks
//  private ProductService service;
//
//  @MockBean
//  private ProductLockService lockService;
//  @Spy
//  private ProductRepository repository;
//  @Mock
//  private RedissonClient redissonClient;
//  @Mock
//  private RLock lock;

//  @BeforeEach
//  void setUp() throws InterruptedException {
//    MockitoAnnotations.openMocks(this);
//    when(redissonClient.getLock(anyString())).thenReturn(lock);
//    when(lock.tryLock(anyLong(), anyLong(), any())).thenReturn(true);
//  }

//  @Test
//  void updateProduct_updateStockWithLock_분산락_테스트() throws Throwable {
//    BigInteger productId = BigInteger.ONE;
//    UpdateProductRequestDto updateProductRequestDto = new UpdateProductRequestDto();
//    updateProductRequestDto.setName("TEST_PRODUCT");
//    updateProductRequestDto.setStock(30);
//    updateProductRequestDto.setPrice(BigDecimal.TEN);
//
//    EventItem.Item item = new EventItem.Item();
//    item.setProductId(BigInteger.ONE);
//    item.setQuantity(300);
//
//    Product product = Product.builder()
//        .id(productId)
//        .name("BASE_PRODUCT")
//        .stock(10)
//        .price(BigDecimal.ONE)
//        .build();
//    repository.save(product);
//
//    when(repository.save(any(Product.class))).thenAnswer(invocation -> {
//      Product savedProduct = invocation.getArgument(0);
//      savedProduct.setId(BigInteger.ONE);
//      savedProduct.setName("BASE_PRODUCT");
//      savedProduct.setStock(10);
//      savedProduct.setPrice(BigDecimal.ONE);
//      savedProduct.setCreatedAt(LocalDateTime.now());
//      savedProduct.setUpdatedAt(LocalDateTime.now());
//      return savedProduct;
//    });
//    when(repository.findById(BigInteger.ONE)).thenReturn(Optional.of(product));
//
//    ExecutorService executorService = Executors.newFixedThreadPool(2);
//    CountDownLatch latch = new CountDownLatch(2);
//
//    Runnable kafkaTask = () -> {
//      try {
//        lockService.updateStockWithLock(item);
//      } catch (Exception e) {
//        log.error("Exception in Kafka task", e);
//      } finally {
//        latch.countDown();
//        ;
//      }
//    };
//
//
//    Runnable apiTask = () -> {
//      try {
//        service.updateProduct(productId, updateProductRequestDto);
//      } catch (Exception e) {
//        log.error("Exception in API task", e);
//      } finally {
//        latch.countDown();
//      }
//    };
//
//    // 동시 실행
//    executorService.submit(kafkaTask);
//    executorService.submit(apiTask);
//
//    latch.await();
//    executorService.shutdown();
//
//    // 하나만 실행되었어야 함
////    verify(repository, times(1)).save(any(Product.class));
//    verify(distributedLockAop, times(1)).lock(any(ProceedingJoinPoint.class));
//    verify(lock, times(1)).tryLock(anyLong(), anyLong(), any());
//    verify(lock, times(1)).unlock();
//  }

  @Test
  void 분산락_테스트() throws InterruptedException {
    int numberOfThreads = 10;
    ExecutorService executorService = Executors.newFixedThreadPool(numberOfThreads);
    CountDownLatch latch = new CountDownLatch(numberOfThreads);

    BigInteger productId = BigInteger.ONE;
    UpdateProductRequestDto updateProductRequestDto = new UpdateProductRequestDto();
    updateProductRequestDto.setName("TEST_PRODUCT");
    updateProductRequestDto.setStock(1);
    updateProductRequestDto.setPrice(BigDecimal.TEN);

    EventItem.Item item = new EventItem.Item();
    item.setProductId(BigInteger.ONE);
    item.setQuantity(1);

    for (int i = 0; i <numberOfThreads; i++) {
      executorService.submit(() -> {
        try {
          productService.updateProduct(BigInteger.ONE, updateProductRequestDto);
          lockService.updateStockWithLock(item);
        } catch (Exception e) {
          e.printStackTrace();
        } finally {
          latch.countDown();
        }
      });
    }

    latch.await();

    Product product = repository.findById(productId)
            .orElse(null);
    System.out.println("현재 재고: " + product.getStock());
  }


}
